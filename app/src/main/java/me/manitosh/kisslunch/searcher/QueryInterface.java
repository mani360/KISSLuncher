package me.manitosh.kisslunch.searcher;

import me.manitosh.kisslunch.ui.ListPopup;

public interface QueryInterface {
    void temporarilyDisableTranscriptMode();
    void updateTranscriptMode(int transcriptMode);

    void launchOccurred();

    void registerPopup(ListPopup popup);
}
