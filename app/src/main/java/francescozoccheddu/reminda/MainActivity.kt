package francescozoccheddu.reminda

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class MainActivity : Activity() {

    private class ListAdapter(context: Context, alarms: List<Alarmer.Alarm>) : ArrayAdapter<Alarmer.Alarm>(context, R.layout.listview_item, alarms) {

        private val timeFormatter : DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

        override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
            val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.listview_item, container, false)
            val alarm: Alarmer.Alarm = getItem(position)!!
            view.findViewById<TextView>(R.id.title).text = alarm.title
            view.findViewById<TextView>(R.id.time).text = timeFormatter.format(alarm.time)
            return view
        }

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun isEnabled(position: Int): Boolean {
            return false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Alarmer.update(applicationContext)
        val alarms = Alarmer.alarms.subList(Alarmer.nextAlarmIndex, Alarmer.alarms.size)
        if (alarms.isEmpty()) {
            setContentView(R.layout.activity_main_empty)
        }
        else {
            setContentView(R.layout.activity_main)
            val list = findViewById<ListView>(R.id.list)
            list.adapter = ListAdapter(this, alarms)
        }
    }

}