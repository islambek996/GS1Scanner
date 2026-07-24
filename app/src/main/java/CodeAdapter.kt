package kg.teksher.gs1scanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kg.teksher.gs1scanner.R

class CodeAdapter(
    private val list: MutableList<String>
) : RecyclerView.Adapter<CodeAdapter.CodeViewHolder>() {

    class CodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCode: TextView = view.findViewById(R.id.txtCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_code, parent, false)
        return CodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeViewHolder, position: Int) {
        holder.txtCode.text = list[position]
    }

    override fun getItemCount(): Int {
        return list.size
    }
}