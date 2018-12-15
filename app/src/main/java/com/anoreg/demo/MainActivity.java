package com.anoreg.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.anoreg.mediapicker.MediaSelector;
import com.anoreg.mediapicker.util.ImageLoader;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView singleImg;
    private GridImageAdapter adapter;

    private ArrayList<String> pathList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.single_btn).setOnClickListener(this);
        findViewById(R.id.multiple_btn).setOnClickListener(this);
        singleImg = findViewById(R.id.single_img);
        RecyclerView recyclerView = findViewById(R.id.recycler);
        initRecyclerView(recyclerView);
    }

    private void initRecyclerView(RecyclerView recyclerView) {
        int spanCount = 2;
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new GridImageAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.single_btn:
                MediaSelector.builder()
                        .setViewMix(false) // pick single photo or multiple photo
                        .start(this, new MediaSelector.IMediaSelectListener() {
                            @Override
                            public void onSelected(ArrayList<String> mediaPaths) {
                                ImageLoader.loadUrl(MainActivity.this, mediaPaths.get(0), singleImg);
                            }
                        });
                break;
            case R.id.multiple_btn:
                MediaSelector.builder()
                        .setMaxSelectCount(10) //max select photo
                        .setUseCamera(true) //use camera or not when in mix mode
                        .setViewMix(true) // pick single photo or multiple photo
                        .setSelected(pathList) //pre-selected photo path list
                        .start(this, new MediaSelector.IMediaSelectListener() {
                            @Override
                            public void onSelected(ArrayList<String> mediaPaths) {
                                pathList = mediaPaths;
                                adapter.notifyDataSetChanged();
                            }
                        });
                break;
        }
    }

    private class GridImageAdapter extends RecyclerView.Adapter<GridImageAdapter.GridItemHolder> {

        @NonNull
        @Override
        public GridItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
            Context context = viewGroup.getContext();
            ImageView view = new ImageView(context);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            view.setLayoutParams(new GridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, displayMetrics.widthPixels / 2));
            return new GridItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GridItemHolder gridItem, int position) {
            String path = pathList.get(position);
            ImageLoader.loadUrl(MainActivity.this, path, gridItem.img);
        }

        @Override
        public int getItemCount() {
            return pathList == null ? 0 : pathList.size();
        }

        class GridItemHolder extends RecyclerView.ViewHolder {
            private ImageView img;

            GridItemHolder(@NonNull ImageView itemView) {
                super(itemView);
                img = itemView;
            }
        }
    }
}
