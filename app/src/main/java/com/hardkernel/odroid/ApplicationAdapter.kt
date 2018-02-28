package com.hardkernel.odroid

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class ApplicationAdapter(context: Context, resource: Int, private val title: List<String>, private val apps: List<ApplicationInfo>) : ArrayAdapter<*>(context, resource, title) {

    fun getViews(position: Int, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(this.context)
        val layout = inflater.inflate(R.layout.applist_dropdown_item_1line, parent, false)

        val image = layout.findViewById(R.id.appIcon) as ImageView
        val viewTitle = layout.findViewById(R.id.appTitle) as TextView

        for (app in apps) {
            if (position == 0) {
                image.setImageResource(android.R.drawable.ic_delete)
                viewTitle.text = "No shortcut"

            } else if (title[position] == "home") {
                image.setImageResource(android.R.drawable.sym_def_app_icon)
                viewTitle.text = "Home"

            } else if (app.packageName == title[position]) {
                val pm = this.context.packageManager
                image.setImageDrawable(app.loadIcon(pm))
                viewTitle.text = app.loadLabel(pm)
            }
        }

        return layout
    }

    override fun getDropDownView(position: Int, convertView: View?,
                                 parent: ViewGroup): View {
        return getViews(position, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getViews(position, parent)
    }
}
