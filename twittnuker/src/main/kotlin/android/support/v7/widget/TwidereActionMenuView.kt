package android.support.v7.widget

import android.content.Context
import android.support.v7.view.menu.TwidereActionMenuItemView
import android.util.AttributeSet
import android.view.View

class TwidereActionMenuView(context: Context, attrs: AttributeSet? = null) : ActionMenuView(context, attrs) {

    fun createActionMenuView(context: Context, attrs: AttributeSet): View {
        return TwidereActionMenuItemView(context, attrs)
    }
}