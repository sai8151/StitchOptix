package com.saidev.stitchoptix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
// Simple Adapter for FAQ RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FAQActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button

        val faqList = listOf(
            FAQItem(
                "What types of embroidery files are supported?",
                "Currently, only DST (Tajima) embroidery files are supported for optimization and saving."
            ),
            FAQItem(
                "What does the optimizer do?",
                "It cleans up the stitch sequence by removing duplicate or unnecessary stitches while preserving the design shape and color stops."
            ),
            FAQItem(
                "Will my design look different after optimization?",
                "No. The optimizer is designed to maintain the same overall appearance, size, and color sequence of your embroidery file."
            ),
            FAQItem(
                "Why does the stitch count decrease?",
                "Redundant or unnecessary stitches are removed. This makes the design cleaner, reduces machine wear, and can shorten stitch time."
            ),
            FAQItem(
                "Where are optimized files saved?",
                "First, the optimized file is written to the app’s internal storage (temporary). Then you’ll be prompted to choose a folder to save it permanently."
            ),
            FAQItem(
                "What if I cancel the save dialog?",
                "The temporary file remains in the app’s internal storage for that session. You can save it again until the app is restarted."
            ),
            FAQItem(
                "Can I undo the optimization?",
                "No. Always keep a copy of your original DST file before running the optimizer."
            ),
            FAQItem(
                "Does this work offline?",
                "Yes. All processing is done on your device. No files are uploaded."
            ),
            FAQItem(
                "I see a 'Python error' message. What should I do?",
                "This usually means the input DST file is corrupted or not readable. Try with another valid DST file."
            )
        )


        val recyclerView: RecyclerView = findViewById(R.id.faq_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FAQAdapter(faqList)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Handle back button press
        return true
    }
}

data class FAQItem(val question: String, val answer: String)


class FAQAdapter(private val faqList: List<FAQItem>) :
    RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    class FAQViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionTextView: TextView = itemView.findViewById(R.id.faq_question)
        val answerTextView: TextView = itemView.findViewById(R.id.faq_answer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FAQViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val faqItem = faqList[position]
        holder.questionTextView.text = faqItem.question
        holder.answerTextView.text = faqItem.answer
    }

    override fun getItemCount(): Int = faqList.size
}
