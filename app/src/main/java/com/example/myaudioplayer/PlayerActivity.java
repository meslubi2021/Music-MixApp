package com.example.myaudioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    TextView songNameTextView, artistNameTextView, durationPlayedTextView, durationTotalTextView;
    ImageView coverArtImageView, nextBtnImageView, prevBtnImageView, backBtnImageView, shuffleBtnImageView, repeatBtnImageView, menuMoreImageView;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;
    int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;

    static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    private  boolean isShuffle = false;
    private boolean isRepeatAll = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initViews();
        getIntenMethod();
        updateUI();
        setSeekBarListener();
        startSeekBarUpdate();

      backBtnImageView.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              finish();
          }
      });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupButtonClickListeners();
    }

    private void setupButtonClickListeners() {
        prevBtnImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevBtnClicked();
            }
        });

        nextBtnImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextBtnClicked();
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseBtnClicked();
            }
        });

        shuffleBtnImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shuffleBtnClicked();
            }
        });

        repeatBtnImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeatBtnClicked();
            }
        });



    }


    private void repeatBtnClicked() {
        isRepeatAll = !isRepeatAll;

        if (isRepeatAll){
            Toast.makeText(getApplicationContext(), "Repetir tudo ativado", Toast.LENGTH_SHORT).show();
            repeatBtnImageView.setColorFilter(Color.GRAY);
        }else {
            Toast.makeText(getApplicationContext(), "Repetir tudo desativado", Toast.LENGTH_SHORT).show();
            repeatBtnImageView.setColorFilter(Color.TRANSPARENT);

        }
    }


    private void shuffleBtnClicked() {
        isShuffle = !isShuffle;

        if (isShuffle){
            Toast.makeText(getApplicationContext(), "Modo Aleatório ativado", Toast.LENGTH_SHORT).show();
            shuffleBtnImageView.setColorFilter(Color.GRAY);


        }else {
            Toast.makeText(getApplicationContext(), "Modo Aleatório desativado", Toast.LENGTH_SHORT).show();
            shuffleBtnImageView.setColorFilter(Color.TRANSPARENT);

        }
    }


    private void prevBtnClicked() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        position = (position - 1) < 0 ? (listSongs.size() - 1) : (position - 1);

        uri = Uri.parse(listSongs.get(position).getPath());
        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        updateUI();
        startSeekBarUpdate();
        mediaPlayer.start();
        updatePlayPauseButton();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (isShuffle) {
                    playNextRandomSong();
                } else {
                    nextBtnClicked();
                }
            }
        });
    }

    private void nextBtnClicked() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        if (isShuffle) {
            position = getRandomPosition();
        } else {
            position = (position + 1) % listSongs.size();
        }

        uri = Uri.parse(listSongs.get(position).getPath());
        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        updateUI();
        startSeekBarUpdate();
        mediaPlayer.start();
        updatePlayPauseButton();
        mediaPlayer.setOnCompletionListener(mp -> {
            if (isShuffle) {
                playNextRandomSong();
            } else {
                nextBtnClicked();
            }
        });
    }




    private void playPauseBtnClicked() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }

        updatePlayPauseButton();
    }

    private void setSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress * 1000);

                    if (progress == seekBar.getMax()){
                        if (isShuffle) {
                            playNextRandomSong();
                        }else {
                            nextBtnClicked();

                        }
                    }

                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void startSeekBarUpdate() {
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    durationPlayedTextView.setText(formattedTime(mCurrentPosition));

                    int totalDuration = mediaPlayer.getDuration() / 1000;
                    durationTotalTextView.setText(formattedTime(totalDuration));

                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void getIntenMethod() {
        position = getIntent().getIntExtra("position", -1);
        listSongs = MainActivity.musicFiles;
        if (listSongs != null) {
            playPauseBtn.setImageResource(R.drawable.baseline_pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(this);
        seekBar.setMax(mediaPlayer.getDuration() / 1000);
        metaData(uri);
    }

    private void updateUI() {
        if (position != -1 && listSongs != null) {
            MusicFiles musicFile = listSongs.get(position);
            songNameTextView.setText(musicFile.getTitle());
            artistNameTextView.setText(musicFile.getArtist());
            seekBar.setMax(mediaPlayer.getDuration() / 1000);
            updatePlayPauseButton();
            metaData(Uri.parse(musicFile.getPath()));
        }
    }

    private void updatePlayPauseButton() {
        if (mediaPlayer.isPlaying()) {
            playPauseBtn.setImageResource(R.drawable.baseline_pause);
        } else {
            playPauseBtn.setImageResource(R.drawable.baseline_play);
        }
    }


    private void initViews() {
        songNameTextView = findViewById(R.id.song_name);
        artistNameTextView = findViewById(R.id.song_artist);
        durationPlayedTextView = findViewById(R.id.durationPlayed);
        durationTotalTextView = findViewById(R.id.durationTotal);
        coverArtImageView = findViewById(R.id.cover_art);
        nextBtnImageView = findViewById(R.id.id_next);
        prevBtnImageView = findViewById(R.id.id_prev);
        backBtnImageView = findViewById(R.id.back_btn);
        shuffleBtnImageView = findViewById(R.id.id_shuffle);
        repeatBtnImageView = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
        shuffleBtnImageView = findViewById(R.id.id_shuffle);
        menuMoreImageView = findViewById(R.id.menuMore);

        isShuffle = false;
        shuffleBtnImageView.setBackgroundColor(Color.TRANSPARENT);
    }

    private void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());

        ImageView gradient = findViewById(R.id.imageViewGredient);
        RelativeLayout mContainer = findViewById(R.id.mContainer);

        gradient.setBackgroundResource(R.drawable.gradient_bg);
        mContainer.setBackgroundResource(R.drawable.main_bg);

        byte[] art = retriever.getEmbeddedPicture();
        Bitmap bitmap = null;
        if (art != null) {
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            ImageAnimation(this, coverArtImageView, bitmap);
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.bewedoc)
                    .into(coverArtImageView);
        }
        applyColors(bitmap);
    }

    private void applyColors(Bitmap bitmap) {
        if (bitmap == null) {
            setGradientColors(0xff000000, 0x00000000);
            setContainerBackground(0xff000000, 0xff000000);
            setTextViewColors(Color.WHITE, Color.DKGRAY);
            return;
        }

        Palette.from(bitmap).generate(palette -> {
            Palette.Swatch swatch = palette.getDominantSwatch();
            if (swatch != null) {
                setGradientColors(swatch.getRgb(), 0x00000000);
                setContainerBackground(swatch.getRgb(), swatch.getRgb());
                setTextViewColors(swatch.getTitleTextColor(), swatch.getBodyTextColor());
            } else {
                setGradientColors(0xff000000, 0x00000000);
                setContainerBackground(0xff000000, 0xff000000);
                setTextViewColors(Color.WHITE, Color.DKGRAY);
            }
        });
    }

    private void setGradientColors(int startColor, int endColor) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{startColor, endColor});
        ImageView gradient = findViewById(R.id.imageViewGredient);
        gradient.setBackground(gradientDrawable);
    }

    private void setContainerBackground(int startColor, int endColor) {
        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{startColor, endColor});
        RelativeLayout mContainer = findViewById(R.id.mContainer);
        mContainer.setBackground(gradientDrawableBg);
    }

    private void setTextViewColors(int titleColor, int bodyColor) {
        TextView songNameTextView = findViewById(R.id.song_name);
        TextView artistNameTextView = findViewById(R.id.song_artist);
        songNameTextView.setTextColor(titleColor);
        artistNameTextView.setTextColor(bodyColor);
    }

    private String formattedTime(int mCurrentPosition) {
        int minutes = mCurrentPosition / 60;
        int seconds = mCurrentPosition % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap) {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (isShuffle) {
            playNextRandomSong();
        } else {
            if (isRepeatAll){
                nextBtnClicked();
            }else {

                if (position < listSongs.size() - 1) {
                    position++;
                    uri = Uri.parse(listSongs.get(position).getPath());
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
                    mediaPlayer.setOnCompletionListener(this);
                    mediaPlayer.start();
                    updateUI();
                    startSeekBarUpdate();
                    updatePlayPauseButton();
                }
            }
        }
    }


    public void playNextRandomSong(){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        position = getRandomPosition();
        uri = Uri.parse(listSongs.get(position).getPath());
        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.start();
        updateUI();
        startSeekBarUpdate();
        updatePlayPauseButton();
    }

    private int getRandomPosition() {
        Random random = new Random();
        return  random.nextInt(listSongs.size());
    }
}