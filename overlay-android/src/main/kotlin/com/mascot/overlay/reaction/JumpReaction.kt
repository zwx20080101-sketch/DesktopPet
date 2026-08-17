package com.mascot.overlay.reaction

import android.view.View

class JumpReaction : Reaction {
    override fun execute(view: View) {
        view.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            .start()
    }
}
