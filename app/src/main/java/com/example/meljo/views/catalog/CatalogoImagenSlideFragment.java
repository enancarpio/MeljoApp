package com.example.meljo.views.catalog;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.meljo.R;

public class CatalogoImagenSlideFragment extends Fragment {

    private static final String ARG_POS = "pos";
    private static final String ARG_TOTAL = "total";
    private static final String ARG_URL = "url";

    private ImageView imageView;
    private VideoView videoView;
    private String url;

    public static CatalogoImagenSlideFragment newInstance(String url, int pos, int total) {
        CatalogoImagenSlideFragment fragment = new CatalogoImagenSlideFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putInt(ARG_POS, pos);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_catalog_image_viewpager, container, false);
        imageView = view.findViewById(R.id.imgCatalogoItem);
        videoView = view.findViewById(R.id.videoCatalogoItem); // 👈 asegúrate de agregarlo al XML

        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);

            // 🔹 Detección simple de video
            boolean esVideo = url != null && (url.endsWith(".mp4") || url.contains("video"));

            if (esVideo) {
                imageView.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);

                videoView.setVideoURI(Uri.parse(url));
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    videoView.start();
                });
            } else {
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .load(url)
                        .placeholder(android.R.color.transparent)
                        .error(android.R.drawable.ic_menu_report_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .dontAnimate()
                        .dontTransform()
                        .into(imageView);

                imageView.setOnClickListener(v -> {
                    CatalogoImagenFullscreenDialogFragment fullscreen =
                            CatalogoImagenFullscreenDialogFragment.newInstance(
                                    url,
                                    getArguments().getInt(ARG_POS),
                                    getArguments().getInt(ARG_TOTAL)
                            );
                    fullscreen.show(getParentFragmentManager(), "imagen_fullscreen");
                });
            }
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Glide.with(this).clear(imageView);
        if (videoView != null) {
            videoView.stopPlayback();
        }
        imageView = null;
        videoView = null;
    }
}
