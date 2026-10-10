package com.drehverschluss.bosschecklistadditions.client;

import com.drehverschluss.bosschecklistadditions.core.BossDrop;

/** Formats the label Boss Checklist draws under a drop icon, e.g. "73%" or "100%x2". */
public final class DropsText {
    private DropsText() {
    }

    public static String label(BossDrop drop) {
        int percent = (int) Math.round(drop.chance() * 100.0);
        percent = Math.max(1, Math.min(100, percent));
        String label = percent + "%";
        if (drop.minCount() == drop.maxCount() && drop.minCount() > 1) {
            label += "x" + drop.minCount();
        }
        return label;
    }
}
