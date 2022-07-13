package jp.co.jimnet.kenshin.ui.wifistatus;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.jimnet.kenshin.R;
import jp.co.jimnet.kenshin.base.BaseActivity;
import jp.co.jimnet.kenshin.databinding.ActivityWifiStatusBinding;
import jp.co.jimnet.kenshin.ui.networkstatus.NetworkOverlayController;

public class WifiStatusActivity extends BaseActivity<ActivityWifiStatusBinding> {

    // ===== コンポーネント
    private TextView page_title_01, text_ok, text_ng, tuusin_tag, wifi_tag, wifi_view, wifi_btn_statu_text;
    private Button sagyou_btn, wifi_kirikae_btn,wifi_back_btn,stop_btn;
    private CheckBox Tuusin_Chbox_On, Tuusin_Chbox_Off;

    private int count, period, delay, NG_count, OK_count, t_count;

    // 接続スタッツ　フラグ
    private boolean Wifi_Fig = false;
    // チェックボックス用　フラグ
    private boolean On_Flg = false; // ON チェックボックス用
    private boolean Off_Flg = false; // OFF チェックボックス用

    private boolean t_Flg = true;
    private boolean ui_Flg = true;
    private Socket socket;

    // ===== タイマー
    private Timer timer;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_wifi_status;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //   setContentView(R.layout.activity_wifi_statu);

        // コンポーネント処理
        init();

        // 結果テキストビュー　非表示
        // Keka_View_show(0);

        // ===============================

        /**
         *  //========= チェックボックス イベント 「ON」
         */
        Tuusin_Chbox_On.setOnClickListener(v -> {
            // チェック状態 取得
            boolean check_On = Tuusin_Chbox_On.isChecked();
            // チェックされてたら
            if (check_On) {
                On_Flg = true; // OK true
                Off_Flg = false; // ng false

                getKenshinApp().configData.realTimeF = 1;
                // スタッツ　false
                networkOverlayController.setOnlineStatus(true);
                // OFF のチェックを外す
                Tuusin_Chbox_Off.setChecked(false);

                // チェック外し 連打防止
            } else if(check_On == false && getKenshinApp().configData.realTimeF == 1) {
                // ON のチェックを付ける
                Tuusin_Chbox_On.setChecked(true);

            } else {
                On_Flg = false; // OK true
                Off_Flg =true;
            }
        });

        /**
         *   //========= チェックボックス イベント 「OFF」
         */
        Tuusin_Chbox_Off.setOnClickListener(v -> {
            // チェック状態 取得
            boolean check_Off = Tuusin_Chbox_Off.isChecked();
            // チェックされてたら
            if (check_Off) {
                Off_Flg = true; // ng false
                On_Flg = false; // OK true
                // ON のチェックを外す
                Tuusin_Chbox_On.setChecked(false);

                getKenshinApp().configData.realTimeF = 0;

                // ================　グローバルスタッツ　判定
                Wi_Fi_Statu_Check(false);

                // チェック外し 連打防止
            } else if(check_Off == false && getKenshinApp().configData.realTimeF == 0) {
                // OFF のチェックを付ける
                Tuusin_Chbox_Off.setChecked(true);

            } else {
                Off_Flg = false; // ng false
                On_Flg = true; // OK true
            }
        });


        // タイマー用
        count = 0;
        t_count = 0;
        delay = 0;
        period = 3000;
        // === ボタンタイトル　テキスト取得 「通信テスト開始」
        String sagyou_btn_text = sagyou_btn.getText().toString();

        /**
         *  通信テストボタンを押したら
         */
        sagyou_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ボタンのテキストを取得
                String sagyou_btn_text = sagyou_btn.getText().toString();
                if (sagyou_btn_text.equals("通信テスト開始"))    {

                    if(timer == null) {
                        OK_count = 0;
                        NG_count = 0;
                    }

                    // === 結果スタッツテキストビュー 表示
                    Keka_View_show(1);

                    // ====== ボタン無効化
                    wifi_back_btn.setEnabled(false);
                    wifi_back_btn.setBackgroundColor(Color.rgb(222, 222, 222));


                    // === 接続中
                    wifi_btn_statu_text.setBackgroundColor(Color.RED);
                    wifi_btn_statu_text.setText("接続中・・・");

                    // === 「テスト中段」ボタン　：　表示 , 「通信テスト開始」ボタン　：　非表示
                    stop_btn.setVisibility(View.VISIBLE);
                    sagyou_btn.setVisibility(View.GONE);

                    // Timer インスタンスを生成
                    timer = new Timer();

                    // 接続確認
                    // new Thread (new Runnable() {
                    timer.scheduleAtFixedRate(new TimerTask() {
                        public void run() {

                            socket = new Socket();
                            // ========= Wifi_Flg の初期値を false にする
                            Wifi_Fig = false;

                            // ========= 本番用 グローバルから取得
                            String ip = getKenshinApp().configData.host; // ホスト
                            int port =  getKenshinApp().configData.port; // ポート

                            InetSocketAddress address = new InetSocketAddress(ip,  port);

                            try {
                                // カウントアップ
                                t_count += 1;

                                socket.connect(address, 2000); // 1.5 秒 タイムアウト設定
                                // 接続 OK 処理
                                if (socket.isConnected() || socket != null) {
                                    Wifi_Fig = true;
                                    Log.d("tag", "Thread　接続テスト 接続OK 成功" + ip);


                                } else if(socket.isClosed() == true || socket == null ) {

                                    Wifi_Fig = false;
                                    Log.d("tag", "Thread　接続テスト 接続NG 失敗" + ip);

                                } //=================== END if

                            } catch (NullPointerException e) {
                                Log.d(" timer.cancel();","NullPointerException  タイマーキャンセル 補足（階層_01）" + e);
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(" timer.cancel();","Exception  タイマーキャンセル 補足（階層_01）" + e);
                            }
                            //======================== UI スレッド ==========================
                            WifiStatusActivity.this.runOnUiThread(new Runnable() {
                                //  runOnUiThread(new Runnable() {
                                @Override
                                public void run () {

                                    count += 1;

                                    if (count <= 10) {
                                        //=============== Wifi_Fig が true だったら OK 処理
                                        if(Wifi_Fig == true) {

                                            // OK カウント コネクト成功時
                                            OK_count += 1;

                                            text_ok.setText(String.valueOf("OK: " + OK_count));
                                            text_ng.setText(String.valueOf("NG: " + NG_count));

                                            Log.d("tag","UI スレッド　接続テスト 接続OK 成功" + ip + ":" + OK_count);

                                            try {
                                                socket.close();
                                                socket = null;
                                                Log.d("tag","UI スレッド　接続テスト 接続OK 成功  socket.close();");

                                                return;
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                        } else if (Wifi_Fig == false) {

                                            // NG カウント コネクト失敗時
                                            NG_count += 1;

                                            text_ng.setText(String.valueOf("NG: " + NG_count));
                                            text_ok.setText(String.valueOf("OK: " + OK_count));

                                            Log.d("tag","UI スレッド　接続テスト 接続NG 失敗" + ip + ":" + NG_count);
                                        }

                                    } else {
                                        // ============================ タイマー停止　処理 ============================
                                        try {

                                            socket.close();
                                            socket = null;

                                            /**
                                             *  結果判定   NG == 0 「接続OK」,  OK == 0 「接続NG」,  OK != 0 > NG != 0 ,
                                             */
                                            // =========== ボタンラベル 、　スタッツテキスト　結果によって表示を変える
                                            // =========== 「接続OK」
                                            if(NG_count == 0 && OK_count > 0) {
                                                // === sagyou_btn 表示 , stop_btn 非表示
                                                Wifi_Test_Btn_Show   (0);

                                                sagyou_btn.setText("接続OK"); // ボタンテキスト
                                                wifi_btn_statu_text.setText("通信テスト終了"); // テスト　スタッツtextview

                                                // ====== ボタン　有効化
                                                wifi_back_btn.setEnabled(true);
                                                wifi_back_btn.setBackgroundColor(Color.rgb(44, 181, 93));

                                                // timerカウント　初期化
                                                count = 0;

                                                // 通信テスト開始　、　テスト中断　ボタン
                                                wifi_btn_statu_text.setBackgroundColor(Color.WHITE);
                                                Log.d("tag","タイマー終了（10回終了）　接続テスト 終了" + ip);

                                                if(timer != null) {
                                                    timer.cancel();
                                                    timer = null;
                                                }

                                                return;

                                                // ============ 「接続NG」
                                            } else if (OK_count == 0 && NG_count > 0) {
                                                // === sagyou_btn 表示 , stop_btn 非表示
                                                Wifi_Test_Btn_Show   (0);

                                                sagyou_btn.setText("接続NG"); // ボタンテキスト
                                                wifi_btn_statu_text.setText("通信テスト終了"); // テスト　スタッツtextview

                                                // ====== ボタン　有効化
                                                wifi_back_btn.setEnabled(true);
                                                wifi_back_btn.setBackgroundColor(Color.rgb(44, 181, 93));

                                                // timerカウント　初期化
                                                count = 0;

                                                // 通信テスト開始　、　テスト中断　ボタン
                                                wifi_btn_statu_text.setBackgroundColor(Color.WHITE);
                                                Log.d("tag","タイマー終了（10回終了）　接続テスト 終了" + ip);

                                                if(timer != null) {
                                                    timer.cancel();
                                                    timer = null;
                                                }

                                                return;

                                                // ============= 「接続不安定」
                                            } else if (OK_count > NG_count && NG_count != 0) {
                                                // === sagyou_btn 表示 , stop_btn 非表示
                                                Wifi_Test_Btn_Show   (0);

                                                sagyou_btn.setText("接続不安定"); // ボタンテキスト
                                                wifi_btn_statu_text.setText("通信テスト終了"); // テスト　スタッツtextview

                                                // ====== ボタン　有効化
                                                wifi_back_btn.setEnabled(true);
                                                wifi_back_btn.setBackgroundColor(Color.rgb(44, 181, 93));

                                                // timerカウント　初期化
                                                count = 0;

                                                // 通信テスト開始　、　テスト中断　ボタン
                                                wifi_btn_statu_text.setBackgroundColor(Color.WHITE);
                                                Log.d("tag","タイマー終了（10回終了）　接続テスト 終了" + ip);

                                                if(timer != null) {
                                                    timer.cancel();
                                                    timer = null;
                                                }

                                                return;
                                            }

                                        } catch (NullPointerException e) {
                                            Log.d(" timer.cancel();","NullPointerException  タイマーキャンセル 補足" + e);
                                            e.printStackTrace();
                                        } catch (Exception e) {
                                            Log.d(" timer.cancel();","Exception  タイマーキャンセル 補足" + e);
                                            e.printStackTrace();
                                        }

                                    }// === END IF

                                }
                            });
                        }

                    },delay, period);

                    //=================== テスト中断時の処理

                } else if (sagyou_btn_text.equals("接続NG")) {

                    // ボタン　、　ビュー　名前初期化
                    sagyou_btn.setText("通信テスト開始");
                    wifi_btn_statu_text.setText("通信テストを開始します");

                    // カウント初期化
                    t_count = 0;
                    count = 0;
                    OK_count = 0;
                    NG_count = 0;

                    text_ok.setText("OK: ");
                    text_ng.setText("NG: ");
                    Log.d("tag","タイマー終了（途中終了）　接続テスト 接続NG 失敗");

                    if(timer == null) {
                        OK_count = 0;
                        NG_count = 0;
                    }

                    return;

                } else if(sagyou_btn_text.equals("接続不安定")) {

                    // ボタン　、　ビュー　名前初期化
                    sagyou_btn.setText("通信テスト開始");
                    wifi_btn_statu_text.setText("通信テストを開始します");

                    // カウント初期化
                    t_count = 0;
                    count = 0;
                    OK_count = 0;
                    NG_count = 0;

                    text_ok.setText("OK: ");
                    text_ng.setText("NG: ");
                    Log.d("tag","タイマー終了（途中終了）　接続テスト 接続NG 失敗");

                    if(timer == null) {
                        OK_count = 0;
                        NG_count = 0;
                    }

                    return;

                } else if (sagyou_btn_text.equals("接続OK")) {

                    // ボタン　、　ビュー　名前初期化
                    sagyou_btn.setText("通信テスト開始");
                    wifi_btn_statu_text.setText("通信テストを開始します");

                    // カウント初期化
                    t_count = 0;
                    count = 0;
                    OK_count = 0;
                    NG_count = 0;

                    text_ok.setText("OK: ");
                    text_ng.setText("NG: ");
                    Log.d("tag","タイマー終了（途中終了）　接続テスト 接続NG 失敗");

                    if(timer == null) {
                        OK_count = 0;
                        NG_count = 0;
                    }

                    return;

                }
                // ================ END if


            } // ===================== onClick END
        });

        /**
         *  テスト中段ボタン
         */
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    // タイマー停止
                    if(timer != null) {
                        timer.cancel();

                        // スレッド停止
                        //    handler.removeCallbacks(runnable);
                        Log.d("スレッド停止", "stop_btn イベント　停止　成功");

                        /**
                         *  結果判定   NG == 0 「接続OK」,  OK == 0 「接続NG」,  OK != 0 > NG != 0 ,
                         */
                        // =========== ボタンラベル 、　スタッツテキスト　結果によって表示を変える
                        // =========== 「接続OK」
                        if(NG_count == 0 && OK_count > 0) {
                            // === sagyou_btn 表示 , stop_btn 非表示
                            Wifi_Test_Btn_Show   (0);
                            sagyou_btn.setText("接続OK"); // ボタンテキスト

                            wifi_btn_statu_text.setBackgroundColor(Color.WHITE);
                            wifi_btn_statu_text.setText("通信テストを中断しました"); // テスト　スタッツtextview

                            // ====== ボタン　有効化
                            wifi_back_btn.setEnabled(true);
                            wifi_back_btn.setBackgroundColor(Color.rgb(44, 181, 93));

                            // ============ 「接続NG」
                        } else if (OK_count == 0 && NG_count > 0) {
                            // === sagyou_btn 表示 , stop_btn 非表示
                            Wifi_Test_Btn_Show   (0);
                            sagyou_btn.setText("接続NG"); // ボタンテキスト

                            wifi_btn_statu_text.setBackgroundColor(Color.WHITE);
                            wifi_btn_statu_text.setText("通信テストを中断しました"); // テスト　スタッツtextview

                            // ====== ボタン　有効化
                            wifi_back_btn.setEnabled(true);
                            wifi_back_btn.setBackgroundColor(Color.rgb(44, 181, 93));

                            // ============= 「接続不安定」
                        } else if (OK_count > NG_count && NG_count != 0) {
                            // === sagyou_btn 表示 , stop_btn 非表示
                            Wifi_Test_Btn_Show   (0);
                            sagyou_btn.setText("接続不安定"); // ボタンテキスト

                            wifi_btn_statu_text.setBackgroundColor(Color.WHITE);
                            wifi_btn_statu_text.setText("通信テストを中断しました"); // テスト　スタッツtextview

                            // ====== ボタン　有効化
                            wifi_back_btn.setEnabled(true);
                            wifi_back_btn.setBackgroundColor(Color.rgb(44, 181, 93));
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();

                }

            }
        });

        /**
         *  切り替えボタン
         */
        wifi_kirikae_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean check_On = Tuusin_Chbox_On.isChecked();
                boolean check_Off = Tuusin_Chbox_Off.isChecked();

                //  ========================== 切り替え  ==========================
                if(getKenshinApp().configData.realTimeF == 0 || t_Flg == false) {
                    // スタッツ ON
                    networkOverlayController.setOnlineStatus(true);
                    Tuusin_Chbox_On.setChecked(true);
                    Tuusin_Chbox_Off.setChecked(false);
                    t_Flg = true;
                    getKenshinApp().configData.realTimeF = 1;

                } else if(getKenshinApp().configData.realTimeF == 1 || t_Flg == true) {
                    //　スタッツ OFF
                    networkOverlayController.setOnlineStatus(false);
                    Tuusin_Chbox_Off.setChecked(true);
                    Tuusin_Chbox_On.setChecked(false);
                    t_Flg = false;
                    getKenshinApp().configData.realTimeF = 0;
                }

            }
        });

        /**
         *  戻る ボタン
         */
        wifi_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();
            }
        });

    } // ============================================================= END onCreate

    /**
     *  コンポーネント処理
     */
    private void init() {

        // Text view テキストビュー
        page_title_01 = findViewById(R.id.page_title_01);
        text_ok = findViewById(R.id.text_ok);
        text_ng = findViewById(R.id.text_ng);
        tuusin_tag = findViewById(R.id.tuusin_tag);
        wifi_tag = findViewById(R.id.wifi_tag);
        wifi_view = findViewById(R.id.wifi_view);

        wifi_btn_statu_text = findViewById(R.id.wifi_btn_statu_text);


        // ====== 接続先　アドレス取得 ======
        wifi_view.setText(getKenshinApp().configData.host);

        // btn ボタン
        sagyou_btn = findViewById(R.id.sagyou_btn); // 通信テスト開始ボタン
        wifi_kirikae_btn = findViewById(R.id.wifi_kirikae_btn); // 切替 ボタン
        wifi_back_btn = findViewById(R.id.wifi_back_btn); // 「戻るボタン」
        stop_btn = findViewById(R.id.stop_btn); // 「テスト中段」ボタン

        // 「テスト中段」ボタン　非表示
        stop_btn.setVisibility(View.GONE);

        // チェックボックス
        Tuusin_Chbox_On = findViewById(R.id.Tuusin_Chbox_On); // ON
        Tuusin_Chbox_Off = findViewById(R.id.Tuusin_Chbox_Off); // OFF

        /**
         *  グローバルの　スタッツ っぽい　 getKenshinApp().configData.realTimeF
         */
        /*
        boolean currentIsOnline = getKenshinApp().configData.realTimeF == 1;
        boolean newIsOnline = !currentIsOnline;
        getKenshinApp().configData.realTimeF = newIsOnline ? 1 : 0;
         */

        if( getKenshinApp().configData.realTimeF == 1) {
            // チェックボックス　初期値　ON = false
            Tuusin_Chbox_On.setChecked(true);
            // チェックボックス　初期値　OFF = false
            Tuusin_Chbox_Off.setChecked(false);
        } else {
            // チェックボックス　初期値　ON = false
            Tuusin_Chbox_On.setChecked(false);
            // チェックボックス　初期値　OFF = false
            Tuusin_Chbox_Off.setChecked(true);
        }


    }

    /**
     *  テキスト　スタッツ 表示・非表示
     */
    private void Keka_View_show(int i) {

        // 非表示
        if (i == 0) {
            text_ok.setVisibility(View.GONE);
            text_ng.setVisibility(View.GONE);
        } else {
            text_ok.setVisibility(View.VISIBLE);
            text_ng.setVisibility(View.VISIBLE);
        }

    } // === Keka_View_show END

    /**
     *    ボタン　表示　,  非表示
     */
    private void Wifi_Test_Btn_Show(int i) {

        if(i == 0) {
            sagyou_btn.setVisibility(View.VISIBLE); // 表示
            stop_btn.setVisibility(View.GONE); // 非表示
        } else {
            sagyou_btn.setVisibility(View.GONE); // 表示
            stop_btn.setVisibility(View.VISIBLE); // 非表示
        }

    }


    //============================= networkOverlayController 関係 ===================
    @Override
    protected void onStart() {
        super.onStart();

        /**
         *   ネットワーク　スタッツ
         */
        if(getKenshinApp().configData.realTimeF == 1) {
            networkOverlayController.setOnlineStatus(false);
            t_Flg = true;
        } else {
            networkOverlayController.setOnlineStatus(true);
            t_Flg = false;
        }

        networkOverlayController.setOnlineStatus(getKenshinApp().configData.realTimeF == 1);
        Log.d("networkOverlayController 値", "onStart getKenshinApp().configData.realTimeF 値::::: " + getKenshinApp().configData.realTimeF);
    }

    private NetworkOverlayController addNetworkStatusOverlay() {
        WindowManager manager = getSystemService(WindowManager.class);

        View view = getLayoutInflater().inflate(R.layout.overlay_network_status, null, false);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;

        manager.addView(view, params);

        return new NetworkOverlayController(view);
    }


    /**
     *
     * @param Statu_OK =>  true : ON  ,  false  :  OFF
     */
    private void Wi_Fi_Statu_Check(boolean Statu_OK) {
        if( getKenshinApp().configData.realTimeF == 1) {
            networkOverlayController.setOnlineStatus(Statu_OK);
        } else {
            networkOverlayController.setOnlineStatus(Statu_OK);
        }
    }



}