package com.hardkernel.odroid

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class ApplicationAdapter(context: Context, resource: Int, private val title: List<String>, private val apps: List<ApplicationInfo>) : ArrayAdapter<*>(context, resource, title) {

    private fun getViews(position: Int, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(this.context)
        val layout = inflater.inflate(R.layout.applist_dropdown_item_1line, parent, false)

        fun setApptile(resource:Any, text:String) {
            val image = layout.findViewById(R.id.appIcon) as ImageView
            val viewTitle = layout.findViewById(R.id.appTitle) as TextView

            when (resource) {
                is Int -> image.setImageResource(resource)
                is Drawable -> image.setImageDrawable(resource)
            }
            viewTitle.text = text
        }
        for (app in apps) {
            when {
                position == 0 ->
                        setApptile(android.R.drawable.ic_delete, "No shortcut")
                title[position] == "home" ->
                        setApptile(android.R.drawable.sym_def_app_icon, "Home")
                app.packageName == title[position] -> {
                    val pm = this.context.packageManager
                    setApptile(app.loadIcon(pm), app.loadLabel(pm).toString())
                }
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
