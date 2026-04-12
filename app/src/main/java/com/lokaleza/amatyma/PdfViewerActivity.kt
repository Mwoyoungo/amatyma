package com.lokaleza.amatyma

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.lokaleza.amatyma.databinding.ActivityPdfViewerBinding

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pdfUrl = intent.getStringExtra("PDF_URL")
        val articleTitle = intent.getStringExtra("ARTICLE_TITLE")

        // Set toolbar title
        binding.toolbar.title = articleTitle ?: "Article"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup WebView
        setupWebView()

        // Load PDF
        if (pdfUrl != null) {
            loadPdf(pdfUrl)
        } else {
            android.widget.Toast.makeText(this, "PDF URL not found", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun loadPdf(pdfUrl: String) {
        binding.progressBar.visibility = View.VISIBLE

        // Use Mozilla PDF.js viewer for reliable PDF rendering
        val pdfViewerUrl = "https://mozilla.github.io/pdf.js/web/viewer.html?file=$pdfUrl"
        binding.webView.loadUrl(pdfViewerUrl)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
