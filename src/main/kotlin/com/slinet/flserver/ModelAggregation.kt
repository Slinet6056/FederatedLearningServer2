package com.slinet.flserver

import org.deeplearning4j.core.storage.StatsStorage
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.ui.api.UIServer
import org.deeplearning4j.ui.model.stats.StatsListener
import org.deeplearning4j.ui.model.storage.FileStatsStorage
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File
import kotlin.concurrent.thread

//用于聚合多个设备的模型
//在这里创建了一个与客户端相同的模型，用于聚合多个设备的模型
//并在模型聚合后进行了一次训练，用于更新显示训练信息的网页
//使用的数据为TrainingData类中的irisData和labelData
//目前这个数据跟客户端用的数据是一样的，但是在实际应用中，可以为其他的数据，用于评估模型的准确性
object ModelAggregation {
    val filePathList = ArrayList<String>()
    private lateinit var model: MultiLayerNetwork
    private lateinit var uiServer: UIServer
    private lateinit var statsStorage: StatsStorage
    private var trainingData: DataSet

    //初始化训练数据
    init {
        val row = TrainingData.irisData.size / 4
        val col = 4
        val irisMatrix = Array(row) { DoubleArray(col) }
        var i = 0
        for (r in 0 until row) {
            for (c in 0 until col) {
                irisMatrix[r][c] = TrainingData.irisData[i++]
            }
        }
        val rowLabel = TrainingData.labelData.size / 3
        val colLabel = 3
        val twodimLabel = Array(rowLabel) { DoubleArray(colLabel) }
        i = 0
        for (r in 0 until rowLabel) {
            for (c in 0 until colLabel) {
                twodimLabel[r][c] = TrainingData.labelData[i++]
            }
        }
        val trainingIn = Nd4j.create(irisMatrix)
        val trainingOut = Nd4j.create(twodimLabel)
        trainingData = DataSet(trainingIn, trainingOut)
    }

    //启动DL4J自带的网页界面，使用文件记录训练信息
    //老师一直想把主界面那个图像图例中的score改成loss，但我也不会（ ﾟ∀｡）
    fun startWebUI() {
        uiServer = UIServer.getInstance()
        statsStorage = FileStatsStorage(File("res/stats.dl4j"))
        model.setListeners(StatsListener(statsStorage, 1))
        uiServer.attach(statsStorage)
    }

    //创建模型
    fun createModel() {
        val inputLayer = DenseLayer.Builder()
            .nIn(4)
            .nOut(20)
            .name("Input")
            .build()

        //隐藏层
        val hiddenLayer = DenseLayer.Builder()
            .nIn(20)
            .nOut(20)
            .name("Hidden")
            .build()

        //输出层
        val outputLayer = OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
            .nIn(20)
            .nOut(3)
            .name("Output")
            .activation(Activation.SOFTMAX) //标准的 softmax 激活函数
            .build()

        val nncBuilder = NeuralNetConfiguration.Builder()
        val seed = 6L
        nncBuilder.seed(seed)
        nncBuilder.activation(Activation.TANH) //标准的双曲正切激活函数
        nncBuilder.weightInit(WeightInit.XAVIER) //初始权重为均值 0, 方差为 2.0/(fanIn + fanOut)的高斯分布

        val listBuilder = nncBuilder.list()
        listBuilder.layer(0, inputLayer)
        listBuilder.layer(1, hiddenLayer)
        listBuilder.layer(2, outputLayer)

        model = MultiLayerNetwork(listBuilder.build())
        model.init()

        Utils.log("Model created")
    }

    //模型聚合
    fun aggregation(layer: Int, alpha: Double) {
        if (filePathList.isEmpty()) return
        val originModel = model
        val filePathList = ArrayList(filePathList)
        ModelAggregation.filePathList.clear()

        //计算模型参数的平均值，使用alpha控制旧的模型占聚合以后模型的权重
        //其实这里的计算方式有点问题，理论上来说假如有三个新的模型要聚合，应该先计算出三个模型的平均值，然后再与旧的模型进行聚合
        //但是这里的计算方式是用循环计算，先计算出旧的模型与第一个新的模型的平均值，然后再计算出这个平均值与第二个新的模型的平均值，以此类推
        //但目前每接收到一个新的模型，就会立即进行一次聚合，所以在设备数量不多的情况下，这种计算方式也是可以的
        for (i in 0 until layer) {
            var paramTable = originModel.paramTable()
            var weights = paramTable[String.format("%d_W", i)]!!
            var bias = paramTable[String.format("%d_b", i)]!!
            var avgWeights = weights.mul(alpha)
            var avgBias = bias.mul(alpha)

            for (filePath in filePathList) {
                var model: MultiLayerNetwork
                try {
                    val file = File(filePath)
                    model = ModelSerializer.restoreMultiLayerNetwork(file, true)
                } catch (e: Exception) {
                    Utils.log(e.message.toString())
                    continue
                }
                paramTable = model.paramTable()
                weights = paramTable[String.format("%d_W", i)]!!
                bias = paramTable[String.format("%d_b", i)]!!
                avgWeights = avgWeights.add(weights.mul(1.0 - alpha).div(filePathList.size))
                avgBias = avgBias.add(bias.mul(1.0 - alpha).div(filePathList.size))
            }

            originModel.setParam(String.format("%d_W", i), avgWeights)
            originModel.setParam(String.format("%d_b", i), avgBias)
        }

        Utils.log("Successfully aggregated ${filePathList.size} models")

        //DL4J的网页界面似乎只在训练后更新，所以这里只好进行一次训练
        thread {
            try {
                val paramTable = originModel.paramTable()
                for (i in 0 until layer) {
                    model.setParam(String.format("%d_W", i), paramTable[String.format("%d_W", i)])
                    model.setParam(String.format("%d_b", i), paramTable[String.format("%d_b", i)])
                }
                model.fit(trainingData)
            } catch (e: Exception) {
                Utils.log(e.message.toString())
            }
        }

        //删除已经聚合的模型文件
        thread {
            for (filePath in filePathList) {
                try {
                    val file = File(filePath)
                    file.delete()
                } catch (e: Exception) {
                    Utils.log(e.message.toString())
                }
            }
        }

        //保存聚合后的模型
        try {
            ModelSerializer.writeModel(originModel, "res/model/trained_model.zip", true)
            Utils.log("Aggregated model saved")
        } catch (e: Exception) {
            Utils.log(e.message.toString())
        }
    }
}