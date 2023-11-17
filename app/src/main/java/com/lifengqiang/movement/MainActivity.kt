
package com.lifengqiang.movement

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.lifengqiang.movement.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    //создать средство запуска намерений для извлечения видеофайла из хранилища устройств
    private var input_video_uri: String? = null
    lateinit var binding: ActivityMainBinding
    val handler = Handler(Looper.getMainLooper())

    var videoMinVal = mutableFloatStateOf(0f)
    var videoMaxVal = mutableFloatStateOf(1f)
    var videoSelectedLowerVal = mutableFloatStateOf(0f)
    var videoSelectedUpperVal = mutableFloatStateOf(1f)

    private val selectVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let {
                input_video_uri = FFmpegKitConfig.getSafParameterForRead(this, it)
                binding.videoView.setVideoURI(it)
/*
                после успешного извлечения видео и правильной настройки повторного видео URI в
                VideyView, запуск VideyView для воспроизведения этого видео
*/
                binding.videoView.start()
            }
        }

    //создать средство запуска намерений для сохранения видеофайла в хранилище устройства
    private val saveVideoLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) {
            it?.let {
                val out = contentResolver.openOutputStream(it)
                val ip: InputStream = FileInputStream(input_video_uri)
/*
                com.google.common.io.ByteStreams, также обеспечивает прямой метод копирования
                все байты из входного потока в выходной поток. Не закрывается или
                промыть любой поток.
                copy (ip, out!!)
*/
                out?.let {
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (ip.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    ip.close()
                    // запись выходного файла (файл скопирован)
                    out.flush()
                    out.close()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeTopBar.setContent {
            TopBar(selectClick = {
                handler.removeCallbacksAndMessages(null)
                selectVideoLauncher.launch("video/*")
            }, saveClick = {
                if (input_video_uri != null) {
                    //passing filename
                    saveVideoLauncher.launch("VID-${System.currentTimeMillis() / 1000}")
                } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG)
                    .show()
            })
        }

        binding.composeControlPanel.setContent {
            Column {
            ScrubberPanel(
            lowerValue = videoSelectedLowerVal.floatValue,
            upperValue = videoSelectedUpperVal.floatValue,
            from = videoMinVal.floatValue,
            to = videoMaxVal.floatValue
        ) { lower, upper ->
            videoSelectedLowerVal.floatValue = lower
            videoSelectedUpperVal.floatValue = upper
            binding.videoView.seekTo(videoSelectedLowerVal.floatValue.toInt() * 1000)
        }
                ControlPanelButtons(
                    firstClick = {
                        if (input_video_uri != null) {
                            slowMotion(
                                videoSelectedLowerVal.floatValue.toInt() * 1000,
                                videoSelectedUpperVal.floatValue.toInt() * 1000
                            )
                        } else Toast.makeText(
                            this@MainActivity,
                            "Please upload video",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    },
                    secondClick = {
                        if (input_video_uri != null) {
                            reverse(
                                videoSelectedLowerVal.floatValue.toInt() * 1000,
                                videoSelectedUpperVal.floatValue.toInt() * 1000
                            )
                        } else Toast.makeText(
                            this@MainActivity,
                            "Please upload video",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    },
                    thirdClick = {
                        if (input_video_uri != null) {
                            fastForward(
                                videoSelectedLowerVal.floatValue.toInt() * 1000,
                                videoSelectedUpperVal.floatValue.toInt() * 1000
                            )
                        } else Toast.makeText(
                            this@MainActivity,
                            "Please upload video",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                )
            }
        }
                /*
                   Настройте VideоView.
                   Для просмотра видео мы будем использовать VideоView.
                */

        binding.videoView.setOnPreparedListener { mp ->
            val duration = mp.duration / 1000
            videoMinVal.floatValue = 0f
            videoMaxVal.floatValue = mp.duration / 1000f
            videoSelectedLowerVal.floatValue = 0f
            videoSelectedUpperVal.floatValue = mp.duration / 1000f
            mp.isLooping = true
            handler.postDelayed(object : Runnable {
                override fun run() {
                    abs(duration - binding.videoView.currentPosition) / 1000

                    if (binding.videoView.currentPosition >= videoSelectedUpperVal.floatValue.toInt() * 1000) {
                        binding.videoView.seekTo(videoSelectedLowerVal.floatValue.toInt() * 1000)
                    }
                    handler.postDelayed(this, 1000)
                }
            }, 0)
        }
    }

    /*
        Метод создания видео с быстрым движением
        */

    private fun fastForward(startMs: Int, endMs: Int) {
        /* startMs - начальное время, с которого мы должны применить эффект.
            EndMs - время окончания, до которого мы должны применить эффект.
            Например, у нас есть видео 5 мин и мы хотим только быстро пересылать часть видео
            Скажем, с 1:00 мин до 2:00 мин, тогда наш startMs будет 1000 мс, а endMs будет 2000 мс.
		 */


        //строка «exe» содержит команду обработки видео. Подробности командования обсуждаются ниже в этом посте.
        // «video_url» - это URL-адрес видео, который требуется отредактировать. Этот URL-адрес можно получить, выбрав любое видео из галереи.
        val folder = cacheDir
        val file = File(folder, System.currentTimeMillis().toString() + ".mp4")
        val exe =
            ("-y -i $input_video_uri -filter_complex [0:v]trim=0:${startMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim=${endMs / 1000},setpts=PTS-STARTPTS[v3];[0:a]atrim=0:${startMs / 1000},asetpts=PTS-STARTPTS[a1];[0:a]atrim=${startMs / 1000}:${endMs / 1000},asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim=${endMs / 1000},asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 -b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast ${file.absolutePath}")
        executeFfmpegCommand(exe, file.absolutePath)
    }
    /*
          Метод реверсирования видео
          Приведенный ниже код совпадает с приведенным выше и изменяется только команда.
    */
    private fun reverse(startMs: Int, endMs: Int) {
        val folder = cacheDir
        val file = File(folder, System.currentTimeMillis().toString() + ".mp4")
        val exe =
            "-y -i $input_video_uri -filter_complex [0:v]trim=0:${endMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=${startMs / 1000},setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 -b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast ${file.absolutePath}"
        executeFfmpegCommand(exe, file.absolutePath)
    }
    /*
             Метод создания замедленного видео для определенной части видео
             Приведенный ниже код аналогичен приведенному выше, но изменяется только команда в строке «exe».
        */
    private fun slowMotion(startMs: Int, endMs: Int) {
        val folder = cacheDir
        val file = File(folder, System.currentTimeMillis().toString() + ".mp4")
        val exe =
            ("-y -i $input_video_uri -filter_complex [0:v]trim=0:${startMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},setpts=2*(PTS-STARTPTS)[v2];[0:v]trim=${endMs / 1000},setpts=PTS-STARTPTS[v3];[0:a]atrim=0:${startMs / 1000},asetpts=PTS-STARTPTS[a1];[0:a]atrim=${startMs / 1000}:${endMs / 1000},asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim=${endMs / 1000},asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 -b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast ${file.absolutePath}")
        executeFfmpegCommand(exe, file.absolutePath)
    }

    private fun executeFfmpegCommand(exe: String, filePath: String) {

        //создание диалогового окна хода выполнения
        val progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        /*
            Здесь мы использовали асинхронное задание для выполнения нашего запроса, потому что, если мы используем обычный метод, диалог хода выполнения
не будет видно. Это происходит потому, что обычный метод и диалог выполнения используют один и тот же поток для выполнения
и в результате только одному разрешается работать за один раз.
Используя задачу Async, мы создаем другой поток, который решает проблему.
         */
        FFmpegKit.executeAsync(exe, { session ->
            val returnCode = session.returnCode
            lifecycleScope.launch(Dispatchers.Main) {
                if (returnCode.isValueSuccess) {
                    /*
                    после успешного выполнения команды ffmpeg,
                    снова настроить URI видео в VideyView
                    */
                    binding.videoView.setVideoPath(filePath)
                    /*
                    измените video_url на filePath, чтобы мы могли выполнять больше манипуляций в
                    результирующее видео. Таким образом мы можем применить столько эффектов, сколько мы хотим в одном видео.
                    На самом деле в хранилище формируется несколько видео, но при использовании приложения
                    ощущение, что мы делаем манипуляции только в одном видео
                    */
                    input_video_uri = filePath
                    //play the result video in VideoView
                    binding.videoView.start()
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Filter Applied", Toast.LENGTH_SHORT).show()
                } else {
                    progressDialog.dismiss()
                    Log.d("TAG", session.allLogsAsString)
                    Toast.makeText(this@MainActivity, "Something Went Wrong!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }, { log ->
            lifecycleScope.launch(Dispatchers.Main) {
                progressDialog.setMessage("Applying Filter..${log.message}")
            }
        }) { statistics -> Log.d("STATS", statistics.toString()) }
    }
    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            deleteTempFiles(cacheDir)
        }
    }



     //Удаление всех временных файлов, созданных во время сеанса приложения


    private fun deleteTempFiles(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    deleteTempFiles(it)
                } else {
                    it.delete()
                }
            }
        }
        return file.delete()
    }

}