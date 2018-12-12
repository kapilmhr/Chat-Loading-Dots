package app.frantic.chatloadingdots

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import app.frantic.loadingdots.LoadingDots



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root) as ViewGroup

        val startButton = findViewById<View>(R.id.start) as Button
        startButton.setOnClickListener{
            startAll(root)
        }

        val endButton = findViewById<View>(R.id.stop) as Button
        endButton.setOnClickListener {
            stopAll(root)
        }

        val addButton = findViewById<View>(R.id.add_programmatically) as Button
        addButton.setOnClickListener{
            addProgrammatically(root)
        }
    }

    private fun startAll(root: ViewGroup) {
        val count = root.childCount
        for (index in 0 until count) {
            val view = root.getChildAt(index)
            if (view is LoadingDots) {
                view.startAnimation()
            } else if (view is ViewGroup) {
                startAll(view)
            }
        }
    }

    private fun stopAll(root: ViewGroup) {
        val count = root.childCount
        for (index in 0 until count) {
            val view = root.getChildAt(index)
            if (view is LoadingDots) {
                view.stopAnimation()
            } else if (view is ViewGroup) {
                stopAll(view)
            }
        }
    }

    private fun addProgrammatically(root: ViewGroup) {
        root.removeAllViews()

        val loadingDots = LoadingDots(this)
        loadingDots.dotsCount = 3
        loadingDots.setDotsSizeRes(R.dimen.LoadingDots_dots_size_default)
        loadingDots.dotsColor = Color.BLUE

        root.addView(
            loadingDots,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }
}
