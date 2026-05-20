package org.futo.inputmethod.latin.uix.actions

import android.view.KeyEvent
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action

/**
 * Sends an Escape key event to the current application.
 * Useful in terminal emulators, vim-like editors, and apps that respond to the Escape key.
 */
val EscapeAction = Action(
    icon = R.drawable.close,
    name = R.string.action_escape_title,
    simplePressImpl = { manager, _ ->
        manager.sendKeyEvent(KeyEvent.KEYCODE_ESCAPE, 0)
    },
    windowImpl = null,
)

/**
 * Sends a Tab key event to the current application.
 * Useful for navigating between form fields or indenting code.
 */
val TabAction = Action(
    icon = R.drawable.sym_keyboard_tab_holo_dark,
    name = R.string.action_tab_title,
    simplePressImpl = { manager, _ ->
        manager.sendKeyEvent(KeyEvent.KEYCODE_TAB, 0)
    },
    windowImpl = null,
)

/**
 * Sends a Ctrl key event to the current application.
 * Some apps (e.g. terminal emulators) respond to raw Ctrl key events.
 * For common Ctrl+key shortcuts (Copy, Paste, Undo, Redo, Select All),
 * use the dedicated actions instead.
 */
val CtrlAction = Action(
    icon = R.drawable.ctrl,
    name = R.string.action_ctrl_title,
    simplePressImpl = { manager, _ ->
        manager.sendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, 0)
    },
    windowImpl = null,
)
