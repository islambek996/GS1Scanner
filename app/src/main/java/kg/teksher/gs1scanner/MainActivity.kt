package kg.teksher.gs1scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

import java.io.File

import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashSet
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

import kg.teksher.gs1scanner.scanner.BarcodeAnalyzer

class MainActivity : AppCompatActivity() {

    //==============================
    // UI
    //==============================

    private lateinit var previewView: PreviewView
    private lateinit var txtCounter: TextView
    private lateinit var txtResult: TextView

    private lateinit var listCodes: ListView

    private lateinit var btnClear: Button
    private lateinit var btnExport: Button

    //==============================
    // Camera
    //==============================

    private val cameraExecutor =
        Executors.newSingleThreadExecutor()

    //==============================
    // Sound
    //==============================

    private val beep =
        ToneGenerator(
            AudioManager.STREAM_NOTIFICATION,
            100
        )

    //==============================
    // Scanner
    //==============================

    private var lastCode = ""

    private val lastScanTime =
        AtomicLong(0)

    //==============================
    // Data
    //==============================

    private val scannedCodes =
        LinkedHashSet<String>()

    private val list =
        ArrayList<String>()

    private lateinit var adapter:
            ArrayAdapter<String>

    //==============================
    // Storage
    //==============================

    private lateinit var prefs:
            SharedPreferences

    companion object {

        private const val PREF_NAME =
            "scanner"

        private const val PREF_CODES =
            "codes"

    }

    //==============================
    // Permission
    //==============================

    private val requestPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                startCamera()

            } else {

                Toast.makeText(
                    this,
                    "Нет доступа к камере",
                    Toast.LENGTH_LONG
                ).show()

            }

        }

    //==============================
    // onCreate
    //==============================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        previewView =
            findViewById(R.id.previewView)

        txtCounter =
            findViewById(R.id.txtCounter)

        txtResult =
            findViewById(R.id.txtResult)

        listCodes =
            findViewById(R.id.listCodes)

        btnClear =
            findViewById(R.id.btnClear)

        btnExport =
            findViewById(R.id.btnExport)

        prefs =
            getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                list
            )

        listCodes.adapter = adapter

        loadHistory()

        updateCounter()

        btnClear.setOnClickListener {

            clearAll()

        }

        btnExport.setOnClickListener {

            exportCSV()

        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            )
            ==
            PackageManager.PERMISSION_GRANTED
        ) {

            startCamera()

        } else {

            requestPermission.launch(
                Manifest.permission.CAMERA
            )

        }

    }

    //==============================
    // Camera
    //==============================

    private fun startCamera() {

        val providerFuture =
            ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({

            val provider =
                providerFuture.get()

            val preview =
                Preview.Builder().build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            val options =
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_DATA_MATRIX
                    )
                    .build()

            val scanner =
                BarcodeScanning.getClient(options)

            val analysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis
                            .STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            analysis.setAnalyzer(
                cameraExecutor,
                BarcodeAnalyzer(scanner) { value ->
                    processBarcode(value)
                }
            )

            provider.unbindAll()

            provider.bindToLifecycle(

                this,

                CameraSelector
                    .DEFAULT_BACK_CAMERA,

                preview,

                analysis

            )

        }, ContextCompat.getMainExecutor(this))

    }

    //==============================
    // Barcode
    //==============================

    private fun processBarcode(
        value: String
    ) {

        val now =
            SystemClock.elapsedRealtime()

        if (
            value == lastCode &&
            now - lastScanTime.get() < 1500
        ) {

            return

        }

        lastCode = value

        lastScanTime.set(now)

        runOnUiThread {

            if (scannedCodes.contains(value)) {

                beep.startTone(
                    ToneGenerator.TONE_PROP_NACK,
                    120
                )


                txtResult.text =
                    "Дубликат"

                return@runOnUiThread

            }

            scannedCodes.add(value)

            list.add(value)

            adapter.notifyDataSetChanged()

            txtResult.text = value

            updateCounter()

            beep.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                120
            )


            saveHistory()

        }

    }
    //==============================
    // Counter
    //==============================

    private fun updateCounter() {

        txtCounter.text =
            "Считано: ${scannedCodes.size}"

    }

    //==============================
    // Save History
    //==============================

    private fun saveHistory() {

        val builder = StringBuilder()

        for (code in list) {

            builder.append(code)

            builder.append("\n")

        }

        prefs.edit()

            .putString(
                PREF_CODES,
                builder.toString()
            )

            .apply()

    }

    //==============================
    // Load History
    //==============================

    private fun loadHistory() {

        val history =
            prefs.getString(
                PREF_CODES,
                ""
            ) ?: ""

        if (history.isEmpty()) {

            return

        }

        val rows =
            history.split("\n")

        for (row in rows) {

            val value = row.trim()

            if (value.isEmpty()) {

                continue

            }

            if (scannedCodes.add(value)) {

                list.add(value)

            }

        }

        adapter.notifyDataSetChanged()

    }

    //==============================
    // Clear
    //==============================

    private fun clearAll() {

        AlertDialog.Builder(this)

            .setTitle("Очистить")

            .setMessage(
                "Удалить все считанные коды?"
            )

            .setPositiveButton("Да") {

                    _, _ ->

                scannedCodes.clear()

                list.clear()

                adapter.notifyDataSetChanged()

                txtResult.text =
                    "Готов к сканированию"

                updateCounter()

                saveHistory()

            }

            .setNegativeButton(
                "Нет",
                null
            )

            .show()

    }

    //==============================
    // Export CSV
    //==============================

    private fun exportCSV() {

        if (list.isEmpty()) {
            Toast.makeText(this, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
            return
        }

        try {

            val dir = File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "Export"
            )

            if (!dir.exists()) {
                dir.mkdirs()
            }

            val file = File(
                dir,
                "GS1_${
                    SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                    ).format(Date())
                }.csv"
            )

            file.bufferedWriter(Charsets.UTF_8).use { writer ->

                list.forEach { code ->

                    val exportCode = code.trimStart(29.toChar())

                    writer.write(exportCode)
                    writer.newLine()
                }
            }

            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Экспорт CSV"))

        } catch (e: Exception) {

            e.printStackTrace()

            android.util.Log.e(
                "EXPORT",
                "Export error",
                e
            )

            Toast.makeText(
                this,
                e.toString(),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //==============================
    // Destroy
    //==============================

    override fun onDestroy() {

        super.onDestroy()

        beep.release()

        cameraExecutor.shutdown()

    }
}