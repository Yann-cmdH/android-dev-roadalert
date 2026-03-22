package com.roadalert.cameroun.ui.history

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.roadalert.cameroun.R
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.AlertStatus
import com.roadalert.cameroun.data.db.entity.SmsStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onCardClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private var items: List<HistoryListItem> = emptyList()

    fun submitList(newItems: List<HistoryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryListItem.Header -> TYPE_HEADER
            is HistoryListItem.Event -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(
                    R.layout.item_history_header,
                    parent, false
                )
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(
                    R.layout.item_history_event,
                    parent, false
                )
                ItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = items[position]) {
            is HistoryListItem.Header ->
                (holder as HeaderViewHolder).bind(item.title)
            is HistoryListItem.Event ->
                (holder as ItemViewHolder).bind(
                    item.event, onCardClick
                )
        }
    }

    // ── HeaderViewHolder ──────────────────────────────────

    inner class HeaderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvGroupTitle: TextView =
            itemView.findViewById(R.id.tvGroupTitle)

        fun bind(title: String) {
            tvGroupTitle.text = title
        }
    }

    // ── ItemViewHolder ────────────────────────────────────

    inner class ItemViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardEvent: CardView =
            itemView.findViewById(R.id.cardEvent)
        private val viewAccent: View =
            itemView.findViewById(R.id.viewAccent)
        private val tvIcon: TextView =
            itemView.findViewById(R.id.tvIcon)
        private val tvTitle: TextView =
            itemView.findViewById(R.id.tvTitle)
        private val tvBadge: TextView =
            itemView.findViewById(R.id.tvBadge)
        private val tvDate: TextView =
            itemView.findViewById(R.id.tvDate)
        private val tvContacts: TextView =
            itemView.findViewById(R.id.tvContacts)
        private val tvGpsStatus: TextView =
            itemView.findViewById(R.id.tvGpsStatus)
        private val tvMapsLink: TextView =
            itemView.findViewById(R.id.tvMapsLink)

        fun bind(
            event: AccidentEvent,
            onCardClick: () -> Unit
        ) {
            val ctx = itemView.context

            // ── Accent gauche + Icône + Titre ─────────────
            if (event.triggerType == "AUTO") {
                viewAccent.setBackgroundColor(0xFFC0392B.toInt())
                tvIcon.text = "!"
                tvTitle.text = ctx.getString(
                    R.string.history_type_accident
                )
            } else {
                viewAccent.setBackgroundColor(0xFF2E86C1.toInt())
                tvIcon.text = "S"
                tvTitle.text = ctx.getString(
                    R.string.history_type_sos
                )
            }

            // ── Badge statut ──────────────────────────────
            val status = event.toAlertStatus()
            val badgeText = when (status) {
                AlertStatus.SENT ->
                    ctx.getString(R.string.history_badge_sent)
                AlertStatus.PARTIAL ->
                    ctx.getString(R.string.history_badge_partial)
                AlertStatus.FAILED ->
                    ctx.getString(R.string.history_badge_failed)
                AlertStatus.PENDING_RETRY ->
                    ctx.getString(R.string.history_badge_retry)
                AlertStatus.PENDING ->
                    ctx.getString(R.string.history_badge_pending)
            }
            val badgeBg = when (status) {
                AlertStatus.SENT ->
                    0xFFEAF3DE.toInt()
                AlertStatus.PARTIAL, AlertStatus.PENDING_RETRY ->
                    0xFFFEF9E7.toInt()
                AlertStatus.FAILED ->
                    0xFFFDEDEC.toInt()
                AlertStatus.PENDING ->
                    0xFFF0F0F0.toInt()
            }
            val badgeText2 = when (status) {
                AlertStatus.SENT ->
                    0xFF27500A.toInt()
                AlertStatus.PARTIAL, AlertStatus.PENDING_RETRY ->
                    0xFF7D6608.toInt()
                AlertStatus.FAILED ->
                    0xFF922B21.toInt()
                AlertStatus.PENDING ->
                    0xFF888780.toInt()
            }

            tvBadge.text = badgeText
            tvBadge.setBackgroundColor(badgeBg)
            tvBadge.setTextColor(badgeText2)

            val badgeBgDrawable = ContextCompat.getDrawable(
                ctx, R.drawable.bg_badge
            )
            tvBadge.background = badgeBgDrawable?.mutate()?.apply {
                setTint(badgeBg)
            }
            tvBadge.setTextColor(badgeText2)

            // ── Date formatée ─────────────────────────────
            tvDate.text = formatDate(event.timestamp)

            // ── Contacts alertés ──────────────────────────
            val sentCount = countSentContacts(event)
            val total = event.smsContactsCount
            val contactText = "$sentCount/$total contact${
                if (total > 1) "s alertes" else " alerte"
            }"
            tvContacts.text = contactText
            tvContacts.setTextColor(
                when {
                    sentCount == total && total > 0 ->
                        0xFF27AE60.toInt()
                    sentCount > 0 ->
                        0xFFF39C12.toInt()
                    else ->
                        0xFFE74C3C.toInt()
                }
            )

            // ── GPS status ────────────────────────────────
            when {
                event.latitude != null -> {
                    tvGpsStatus.text = if (event.isPositionApproximate)
                        ctx.getString(R.string.history_gps_approx)
                    else
                        ctx.getString(R.string.history_gps_precise)
                    tvGpsStatus.setTextColor(
                        if (event.isPositionApproximate)
                            0xFFF39C12.toInt()
                        else
                            0xFF27AE60.toInt()
                    )

                    // Lien Maps visible
                    val mapsUrl = String.format(
                        Locale.US,
                        "maps.google.com/?q=%.4f,%.4f",
                        event.latitude,
                        event.longitude
                    )
                    val displayUrl = if (mapsUrl.length > 30)
                        mapsUrl.take(30) + "..." else mapsUrl
                    tvMapsLink.text = displayUrl
                    tvMapsLink.visibility = View.VISIBLE
                    tvMapsLink.setOnClickListener {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://$mapsUrl")
                        )
                        ctx.startActivity(intent)
                    }
                }
                else -> {
                    tvGpsStatus.text = ctx.getString(
                        R.string.history_gps_unavailable
                    )
                    tvGpsStatus.setTextColor(0xFF95A5A6.toInt())
                    tvMapsLink.visibility = View.GONE
                }
            }

            // ── Tap sur la card ───────────────────────────
            cardEvent.setOnClickListener {
                onCardClick()
            }
        }

        // ── Helpers ───────────────────────────────────────

        private fun countSentContacts(
            event: AccidentEvent
        ): Int {
            var count = 0
            if (event.smsContact1Status ==
                SmsStatus.SENT.name) count++
            if (event.smsContact2Status ==
                SmsStatus.SENT.name) count++
            if (event.smsContact3Status ==
                SmsStatus.SENT.name) count++
            return count
        }

        private fun formatDate(timestamp: Long): String {
            if (timestamp <= 0L) return "—"

            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val oneDay = 24 * 60 * 60 * 1000L

            return when {
                diff < oneDay -> {
                    val sdf = SimpleDateFormat(
                        "HH:mm", Locale.FRANCE
                    )
                    "Aujourd'hui a ${sdf.format(Date(timestamp))}"
                }
                diff < 2 * oneDay -> {
                    val sdf = SimpleDateFormat(
                        "HH:mm", Locale.FRANCE
                    )
                    "Hier a ${sdf.format(Date(timestamp))}"
                }
                diff < 7 * oneDay -> {
                    val sdf = SimpleDateFormat(
                        "EEEE 'a' HH:mm", Locale.FRANCE
                    )
                    sdf.format(Date(timestamp))
                        .replaceFirstChar { it.uppercase() }
                }
                else -> {
                    val sdf = SimpleDateFormat(
                        "dd/MM 'a' HH:mm", Locale.FRANCE
                    )
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}