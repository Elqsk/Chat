package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 이게 true여야만 소켓을 연결하는 코드를 지나갈 수 있다. onStop()에서 false로 하고 onStart()에서
    // true로 하는 코드가 없는 것으로 보아, 앱이 처음 실행되었을 때 한 번만 소켓을 연결하게 하려는 일회성의
    // 목적인 것 같다.
    boolean isReadyForConnection = true;
    // 위의 변수와 마찬가지
    boolean isReadyForRead = true;

    ArrayList<ItemContract> dataSet;
    RecyclerView mListView;
    ListAdapter mListAdapter;

    EditText editBox;
    ImageView sendButton;

    // 소켓 연결 여부
    boolean isConnected = false;

    // 서버와 클라이언트는 컨베이어 벨트로 연결되어 있는데, 그 연결된 부분을 소켓이라고 함
    Socket mSocket;
    // 클라이언트(내) 쪽으로 작동하는 컨베이어 벨트
    // 여기에 상대가 보낸 메시지가 실려옴
    BufferedInputStream mInputStream;
    // 서버 쪽으로 작동하는 컨베이어 벨트
    // 여기에 메시지를 실어 보내면 서버가 상대에게 가는 벨트에 실어 보냄
    BufferedOutputStream mOutputStream;

    // 도착한 메시지를 읽어들임
    ReaderThread readerThread;
    // 메시지를 보냄
    WriterThread writerThread;
    Handler writerHandler;

    String SERVER_IP = "172.30.1.20";
    int SERVER_PORT = 7070;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.main_list_view);

        editBox = findViewById(R.id.main_edit_box);
        sendButton = findViewById(R.id.main_send_button);

        sendButton.setOnClickListener(this);

        dataSet = new ArrayList<>();
        mListAdapter = new ListAdapter(dataSet, this, R.layout.item_list);

        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        mListView.setAdapter(mListAdapter);

        sendButton.setEnabled(false);
        editBox.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("kkang", "MainActivity / onStart()");
        Log.d("kkang", " ");

        new ConnectorThread().start();
    }

    // 소켓 연결하는 코드
    class ConnectorThread extends Thread {
        @Override
        public void run() {
            super.run();

            Log.d("kkang", "MainActivity ) ConnectorThread");

            while (isReadyForConnection) {
                if (!isConnected) {
                    SocketAddress mAddress = new InetSocketAddress(SERVER_IP, SERVER_PORT);

                    Log.d("kkang", "MainActivity / ConnectorThread [ 연결중 ] SocketAddress mAddress: " + mAddress);
                    Log.d("kkang", " ");

                    mSocket = new Socket();
                    try {
                        mSocket.connect(mAddress, 10 * 1000);

                        mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
                        mInputStream = new BufferedInputStream(mSocket.getInputStream());

                        if (writerThread != null) {
                            writerHandler.getLooper().quit();
                        }
                        writerThread = new WriterThread();
                        writerThread.start();

                        if (readerThread != null) {
                            isReadyForRead = false;
                        }
                        readerThread = new ReaderThread();
                        readerThread.start();

                        isConnected = true;

                        Message msg = new Message();
                        msg.what = 10;

                        mHandler.sendMessage(msg);

                    } catch (IOException e) {
                        e.printStackTrace();

                        Log.e("kkang", "ConnectorThread [ 연결 실패 ] IOException e: " + e);
                        Log.e("kkang", " ");
                    }
                } else {
                    SystemClock.sleep(1000);
                }
            }
        }
    }

    class ReaderThread extends Thread {

        @Override
        public void run() {
            super.run();

            Log.d("kkang", "MainActivity ) ReaderThread");

            while (isReadyForRead) {
                try {
                    byte[] bytes = new byte[1024];
                    int length = mInputStream.read(bytes);

                    Log.d("kkang", "MainActivity ) ReaderThread / byte[] bytes: " + bytes);
                    Log.d("kkang", "MainActivity ) ReaderThread / bytes.length: " + bytes.length);
                    Log.d("kkang", "MainActivity ) ReaderThread / int length: " + length);

                    if (length > 0) {
                        String content;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            content = new String(bytes, 0, length, StandardCharsets.UTF_8);
                        } else {
                            content = new String(bytes, 0, length, "utf-8");
                        }
                        Log.d("kkang", "MainActivity ) ReaderThread / String content: " + content);
                        Log.d("kkang", " ");

                        if (content != null && !content.equals("")) {
                            Message msg = new Message();
                            msg.what = 100;
                            msg.obj = content;

                            mHandler.sendMessage(msg);
                        }
                    } else {
                        isReadyForRead = false;

                        isConnected = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    Log.e("kkang", "MainActivity ) ReaderThread / IOException e: " + e);
                    Log.e("kkang", " ");
                }
            }
            Message msg = new Message();
            msg.what = 20;

            mHandler.sendMessage(msg);
        }
    }

    Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 10) {
                Log.d("kkang", "MainActivity / readerHandler / 소켓 연결됨");
                Log.d("kkang", " ");

                editBox.setEnabled(true);
                sendButton.setEnabled(true);

            } else if (msg.what == 20) {
                Log.e("kkang", "MainActivity / readerHandler / 소켓 연결 실패");
                Log.e("kkang", " ");

                editBox.setEnabled(false);
                sendButton.setEnabled(false);

            } else if (msg.what == 100) {
                Log.d("kkang", "MainActivity / readerHandler / 메시지가 도착했습니다.");
                Log.d("kkang", " ");

                printMessage("you", (String) msg.obj);

            } else if (msg.what == 200) {
                Log.d("kkang", "MainActivity / readerHandler / 메시지를 보냈습니다.");
                Log.d("kkang", " ");

                printMessage("me", (String) msg.obj);
            }
        }
    };

    private void printMessage(String name, String text) {
        // 전송하거나 받은 메시지 출력
        ItemContract mContract = new ItemContract();
        mContract.setUsername(name);
        mContract.setContent(text);

        dataSet.add(mContract);

        Log.d("kkang", "MainActivity / printMessage() / String name: " + name);
        Log.d("kkang", "MainActivity / printMessage() / String text: " + text);
        Log.d("kkang", "MainActivity / printMessage() / dataSet.size(): " + dataSet.size());
        Log.d("kkang", " ");

        mListAdapter.notifyItemInserted(dataSet.size() - 1);
//        mListAdapter.notifyDataSetChanged();
    }

    class WriterThread extends Thread {

        @Override
        public void run() {
            super.run();

            Log.d("kkang", "MainActivity ) WriterThread");

            Looper.prepare();

            writerHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);

                    Log.d("kkang", "MainActivity ) WriterThread / (String) msg.obj: " + (String) msg.obj);
                    Log.d("kkang", " ");

                    try {
                        mOutputStream.write(
                                ((String) msg.obj).getBytes()
                        );
                        mOutputStream.flush();

                        Message message = new Message();
                        message.what = 200;
                        message.obj = msg.obj;

                        mHandler.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();

                        isConnected = false;

                        isReadyForRead = false;

                        writerHandler.getLooper().quit();

                        Log.e("kkang", "MainActivity / WriterThread / IOException e: " + e);
                        Log.e("kkang", " ");
                    }
                }
            };
            Looper.loop();
        }
    }

    @Override
    public void onClick(View v) {
        // 메시지 전송 버튼 클릭
        String text = editBox.getText().toString();

        Log.d("kkang", "MainActivity / onClick() / String text: " + text);
        Log.d("kkang", " ");

        if (!text.trim().equals("")) {
            Message msg = new Message();
            msg.obj = text;

            writerHandler.sendMessage(msg);

            editBox.setText("");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d("kkang", "MainActivity / onStop()");
        Log.d("kkang", " ");

        isReadyForConnection = false;

        isConnected = false;

        if (mSocket != null) {
            isReadyForRead = false;

            writerHandler.getLooper().quit();
            try {
                mSocket.close();
                mOutputStream.close();
                mInputStream.close();

            } catch (IOException e) {
                e.printStackTrace();

                Log.e("kkang", "MainActivity / onStop() / IOException e: " + e);
                Log.e("kkang", " ");
            }
        }
    }
}