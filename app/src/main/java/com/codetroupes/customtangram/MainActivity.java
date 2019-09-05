package com.codetroupes.customtangram;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.codetroupes.customtangram.rx.JSONArrayObservable;
import com.codetroupes.customtangram.rx.lifecycle.ActivityLFEvent;
import com.codetroupes.customtangram.rx.lifecycle.LifeCycleProviderImpl;
import com.codetroupes.customtangram.view.TowTwoTestView;
import com.codetroupes.customtangram.view.OneTwoTestView;
import com.codetroupes.customtangram.view.OneOneTestView;
import com.codetroupes.customtangram.view.TwoOneTestView;
import com.libra.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import com.tmall.wireless.tangram.TangramBuilder;
import com.tmall.wireless.tangram.TangramEngine;

import com.tmall.wireless.tangram.structure.BaseCell;
import com.tmall.wireless.tangram.support.BannerSupport;

import com.tmall.wireless.tangram.support.async.CardLoadSupport;
import com.tmall.wireless.tangram.util.IInnerImageSetter;
import com.tmall.wireless.vaf.framework.VafContext;
import com.tmall.wireless.vaf.virtualview.view.image.ImageBase;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import com.squareup.picasso.Picasso.LoadedFrom;
import com.tmall.wireless.tangram.dataparser.concrete.Card;

import com.tmall.wireless.tangram.op.AppendGroupOp;
import com.tmall.wireless.tangram.op.LoadGroupOp;
import com.tmall.wireless.tangram.op.LoadMoreOp;
import com.tmall.wireless.tangram.op.ParseSingleGroupOp;


import com.tmall.wireless.tangram.support.RxBannerScrolledListener.ScrollEvent;
import com.tmall.wireless.vaf.virtualview.Helper.ImageLoader.IImageLoaderAdapter;
import com.tmall.wireless.vaf.virtualview.Helper.ImageLoader.Listener;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by longerian on 2018/3/14.
 *
 * @author longerian
 * @date 2018/03/14
 */

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Handler mMainHandler;
    private TangramEngine engine;
    private TangramBuilder.InnerBuilder builder;
    private RecyclerView recyclerView;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private LifeCycleProviderImpl<ActivityLFEvent> mActivityLFEventLifeCycleProvider = new LifeCycleProviderImpl<>(
            ActivityLFEvent.ACTIVITY_LIFECYCLE);

    private static class ImageTarget implements Target {

        ImageBase mImageBase;

        Listener mListener;

        public ImageTarget(ImageBase imageBase) {
            mImageBase = imageBase;
        }

        public ImageTarget(Listener listener) {
            mListener = listener;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
            mImageBase.setBitmap(bitmap, true);
            if (mListener != null) {
                mListener.onImageLoadSuccess(bitmap);
            }
            Log.d("TangramActivity", "onBitmapLoaded " + from);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            if (mListener != null) {
                mListener.onImageLoadFailed();
            }
            Log.d("TangramActivity", "onBitmapFailed ");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.d("TangramActivity", "onPrepareLoad ");
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityLFEventLifeCycleProvider.emitNext(ActivityLFEvent.CREATE);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.main_view);

        //Step 1: init tangram
        TangramBuilder.init(this.getApplicationContext(), new IInnerImageSetter() {
            @Override
            public <IMAGE extends ImageView> void doLoadImageUrl(@NonNull IMAGE view,
                                                                 @Nullable String url) {
                Picasso.with(MainActivity.this).load(url).into(view);
            }
        }, ImageView.class);

        //Tangram.switchLog(true);
        mMainHandler = new Handler(getMainLooper());

        //Step 2: register build=in cells and cards
        builder = TangramBuilder.newInnerBuilder(this);

        //Step 3: register business cells and cards
        builder.registerCell(1, OneOneTestView.class);
        builder.registerCell(2, OneTwoTestView.class);
        builder.registerCell(3, TwoOneTestView.class);
        builder.registerCell(4, TowTwoTestView.class);


        builder.registerVirtualView("vvtest");
        //Step 4: new engine
        engine = builder.build();
        engine.setSupportRx(true);
        engine.setVirtualViewTemplate(VVTEST.BIN);
        engine.getService(VafContext.class).setImageLoaderAdapter(new IImageLoaderAdapter() {

            private List<MainActivity.ImageTarget> cache = new ArrayList<MainActivity.ImageTarget>();

            @Override
            public void bindImage(String uri, final ImageBase imageBase, int reqWidth, int reqHeight) {
                RequestCreator requestCreator = Picasso.with(MainActivity.this).load(uri);
                Log.d("TangramActivity", "bindImage request width height " + reqHeight + " " + reqWidth);
                if (reqHeight > 0 || reqWidth > 0) {
                    requestCreator.resize(reqWidth, reqHeight);
                }
                MainActivity.ImageTarget imageTarget = new MainActivity.ImageTarget(imageBase);
                cache.add(imageTarget);
                requestCreator.into(imageTarget);
            }

            @Override
            public void getBitmap(String uri, int reqWidth, int reqHeight, final Listener lis) {
                RequestCreator requestCreator = Picasso.with(MainActivity.this).load(uri);
                Log.d("TangramActivity", "getBitmap request width height " + reqHeight + " " + reqWidth);
                if (reqHeight > 0 || reqWidth > 0) {
                    requestCreator.resize(reqWidth, reqHeight);
                }
                MainActivity.ImageTarget imageTarget = new MainActivity.ImageTarget(lis);
                cache.add(imageTarget);
                requestCreator.into(imageTarget);
            }
        });
        Utils.setUedScreenWidth(720);

        //Step 5: add card load support if you have card that loading cells async
        CardLoadSupport cardLoadSupport = new CardLoadSupport();
        engine.addCardLoadSupport(cardLoadSupport);


        engine.addSimpleClickSupport(new SampleClickSupport());
        BannerSupport bannerSupport = new BannerSupport();
        engine.register(BannerSupport.class, bannerSupport);
        Disposable dsp1 = bannerSupport.observeSelected("banner1").subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d("TangramActivity", "1 selected " + integer);
            }
        });
        mCompositeDisposable.add(dsp1);

        //Step 6: enable auto load more if your page's data is lazy loaded
        engine.enableAutoLoadMore(true);

        //Step 7: bind recyclerView to engine
        engine.bindView(recyclerView);

        //Step 8: listener recyclerView onScroll event to trigger auto load more
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                engine.onScrolled();
            }
        });

        //Step 9: set an offset to fix card
        engine.getLayoutManager().setFixOffset(0, 40, 0, 0);


        Disposable dsp8 = Observable.create(new ObservableOnSubscribe<JSONArray>() {
            @Override
            public void subscribe(ObservableEmitter<JSONArray> emitter) throws Exception {
                String json = new String(getAssertsFile(getApplicationContext(), "data.json"));
                JSONArray data = null;
                try {
                    data = new JSONArray(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                emitter.onNext(data);
                emitter.onComplete();
            }
        }).flatMap(new Function<JSONArray, ObservableSource<JSONObject>>() {
            @Override
            public ObservableSource<JSONObject> apply(JSONArray jsonArray) throws Exception {
                return JSONArrayObservable.fromJsonArray(jsonArray);
            }
        }).map(new Function<JSONObject, ParseSingleGroupOp>() {
            @Override
            public ParseSingleGroupOp apply(JSONObject jsonObject) throws Exception {
                return new ParseSingleGroupOp(jsonObject, engine);
            }
        }).compose(engine.getSingleGroupTransformer())
                .filter(new Predicate<Card>() {
                    @Override
                    public boolean test(Card card) throws Exception {
                        return card.isValid();
                    }
                }).map(new Function<Card, AppendGroupOp>() {
                    @Override
                    public AppendGroupOp apply(Card card) throws Exception {

                        return new AppendGroupOp(card);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(engine.asAppendGroupConsumer(), new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
        mCompositeDisposable.add(dsp8);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mActivityLFEventLifeCycleProvider.emitNext(ActivityLFEvent.START);
        Observable.interval(1, TimeUnit.SECONDS)
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        Log.i(TAG, "Unsubscribing subscription from onStart()");
                    }
                })
                .compose(mActivityLFEventLifeCycleProvider.<Long>bindUntil(ActivityLFEvent.DESTROY))
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long num) throws Exception {
                        Log.i(TAG, "Started in onStart(), running until in onDestroy(): " + num);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityLFEventLifeCycleProvider.emitNext(ActivityLFEvent.RESUME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityLFEventLifeCycleProvider.emitNext(ActivityLFEvent.PAUSE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mActivityLFEventLifeCycleProvider.emitNext(ActivityLFEvent.STOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityLFEventLifeCycleProvider.emitNext(ActivityLFEvent.DESTROY);
        if (engine != null) {
            engine.destroy();
        }
        mCompositeDisposable.dispose();
    }

    public static byte[] getAssertsFile(Context context, String fileName) {
        InputStream inputStream = null;
        AssetManager assetManager = context.getAssets();
        try {
            inputStream = assetManager.open(fileName);
            if (inputStream == null) {
                return null;
            }

            BufferedInputStream bis = null;
            int length;
            try {
                bis = new BufferedInputStream(inputStream);
                length = bis.available();
                byte[] data = new byte[length];
                bis.read(data);

                return data;
            } catch (IOException e) {

            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (Exception e) {

                    }
                }
            }

            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
