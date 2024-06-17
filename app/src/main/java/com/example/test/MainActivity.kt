package com.example.test

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var movementDataGraph: LineChart
    private lateinit var historyGraph: LineChart
    private lateinit var pdfButton: Button
    lateinit var sessionSpinner: Spinner
    lateinit var date: Spinner
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val REQUEST_WRITE_STORAGE = 112
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        movementDataGraph = findViewById(R.id.movement_data_graph)
        historyGraph = findViewById(R.id.history_graph_image)
        pdfButton = findViewById(R.id.pdf_button)
        sessionSpinner = findViewById(R.id.session_spinner)

        //session
        val sessions = Array(31) { index -> "Session ${index + 1}" }
        // Create adapter for Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sessions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sessionSpinner.adapter = adapter



        db = FirebaseFirestore.getInstance()
        loadGraphData()
        loadMovementDataGraph()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE
            )
        }

        pdfButton.setOnClickListener {
            createPdf()
        }
    }

    private fun loadMovementDataGraph() {
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 20f))
        entries.add(Entry(1f, 24f))
        entries.add(Entry(2f, 2f))
        entries.add(Entry(3f, 10f))
        entries.add(Entry(4f, 28f))

        val dataSet = LineDataSet(entries, "movement data")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val lineData = LineData(dataSet)
        movementDataGraph.data = lineData
        movementDataGraph.invalidate()

        val xAxis: XAxis = movementDataGraph.xAxis
        val leftAxis: YAxis = movementDataGraph.axisLeft
        val rightAxis: YAxis = movementDataGraph.axisRight

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        leftAxis.setDrawGridLines(false)
        rightAxis.setDrawGridLines(false)
        rightAxis.isEnabled = false
    }

    private fun loadGraphData() {
        db.collection("test").get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val historyEntries = ArrayList<Entry>()
                    for (document in task.result!!) {
                        val history = document.get("history") as Map<String, Double>?
                        history?.entries?.forEachIndexed { index, entry ->
                            historyEntries.add(Entry(index.toFloat(), entry.value.toFloat()))
                        }
                    }

                    val historyDataSet = LineDataSet(historyEntries, "history")
                    historyDataSet.color = Color.BLUE

                    val historyLineData = LineData(historyDataSet)
                    historyGraph.data = historyLineData
                    historyGraph.invalidate()
                } else {
                    Log.e("adi", "Error getting documents: ", task.exception)
                }
            }
    }

    private fun createPdf() {
        val fileName = "Assingment.pdf"
        val filePath = getExternalFilesDir(null)?.absolutePath + File.separator + fileName

        try {
            val pdfDocument = PdfDocument(PdfWriter(filePath))
            val document = Document(pdfDocument)

            document.add(Paragraph("Movement Data Graph:"))
            document.add(Image(ImageDataFactory.create(chartToBitmap(movementDataGraph))))
            document.add(Image(ImageDataFactory.create(chartToBitmap(historyGraph))))
            document.add(Paragraph("\nHistory Graph:"))

            document.close()
            pdfDocument.close()

            Toast.makeText(this, "PDF saved to $filePath", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun chartToBitmap(chart: LineChart): ByteArray {
        val width = chart.width
        val height = chart.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)

        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted
                } else {

                }
                return
            }
        }
    }
}
