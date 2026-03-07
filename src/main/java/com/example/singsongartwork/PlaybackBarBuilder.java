package com.example.singsongartwork;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

/**
 * Builder for the playback control bar component.
 * Encapsulates all playback UI creation and state management.
 */
public class PlaybackBarBuilder {
    private Label nowPlayingLabel;
    private Label playbackTimeLabel;
    private Slider playbackSlider;
    private Button playbackPlayPauseButton;
    private Button playbackStopButton;
    private boolean scrubbingPlayback = false;

    private final Runnable onPlayPauseClicked;
    private final Runnable onStopClicked;
    private final java.util.function.Consumer<Duration> onSeekChanged;

    public PlaybackBarBuilder(
            Runnable onPlayPauseClicked,
            Runnable onStopClicked,
            java.util.function.Consumer<Duration> onSeekChanged) {
        this.onPlayPauseClicked = onPlayPauseClicked;
        this.onStopClicked = onStopClicked;
        this.onSeekChanged = onSeekChanged;
    }

    /**
     * Build and return the complete playback bar.
     */
    public HBox buildBar() {
        nowPlayingLabel = new Label("Now Playing: -");
        playbackTimeLabel = new Label("00:00 / 00:00");

        playbackPlayPauseButton = new Button("▶");
        playbackPlayPauseButton.setOnAction(e -> onPlayPauseClicked.run());

        playbackStopButton = new Button("■");
        playbackStopButton.setOnAction(e -> onStopClicked.run());
        playbackStopButton.setDisable(true);

        playbackSlider = new Slider(0, 0, 0);
        playbackSlider.setPrefWidth(320);
        playbackSlider.setOnMousePressed(e -> scrubbingPlayback = true);
        playbackSlider.setOnMouseReleased(e -> {
            onSeekChanged.accept(Duration.seconds(playbackSlider.getValue()));
            scrubbingPlayback = false;
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox playbackBar = new HBox(10);
        playbackBar.getStyleClass().add("status-bar");
        playbackBar.setPadding(new Insets(8, 14, 8, 14));
        playbackBar.setAlignment(Pos.CENTER_LEFT);
        playbackBar.getChildren().addAll(
                nowPlayingLabel,
                spacer,
                playbackSlider,
                playbackTimeLabel,
                playbackPlayPauseButton,
                playbackStopButton
        );

        return playbackBar;
    }

    /**
     * Update the now-playing label.
     */
    public void setNowPlaying(String text) {
        if (nowPlayingLabel != null) {
            nowPlayingLabel.setText(text);
        }
    }

    /**
     * Update the playback time display.
     */
    public void setTime(String current, String total) {
        if (playbackTimeLabel != null) {
            playbackTimeLabel.setText(current + " / " + total);
        }
    }

    /**
     * Update the play button state (▶ or ⏸).
     */
    public void setPlayingState(boolean playing) {
        if (playbackPlayPauseButton != null) {
            playbackPlayPauseButton.setText(playing ? "⏸" : "▶");
        }
    }

    /**
     * Enable/disable stop button.
     */
    public void setStopEnabled(boolean enabled) {
        if (playbackStopButton != null) {
            playbackStopButton.setDisable(!enabled);
        }
    }

    /**
     * Get the seek slider for external updates.
     */
    public Slider getSeekSlider() {
        return playbackSlider;
    }


    /**
     * Set slider max duration.
     */
    public void setMaxDuration(double seconds) {
        if (playbackSlider != null) {
            playbackSlider.setMax(Math.max(seconds, 0));
        }
    }

    /**
     * Update slider position (only if not scrubbing).
     */
    public void updateSliderPosition(double seconds) {
        if (playbackSlider != null && !scrubbingPlayback) {
            playbackSlider.setValue(seconds);
        }
    }

    /**
     * Reset slider to zero.
     */
    public void resetSlider() {
        if (playbackSlider != null) {
            playbackSlider.setValue(0);
        }
    }
}

