package com.example.myq.fchinanet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Created by myq on 2016/10/15.
 */

public class Login {
    Context context;
    String account,passwd,lastIp,author,id,serverId,brasIp,wanIp;
    boolean isFull=false;
    Handler handler=null;
    public Login(Context context, String account, String passwd,Handler handler){
        this.context=context;
        this.account=account;
        this.passwd=passwd;
        this.handler=handler;
        SharedPreferences sp=context.getSharedPreferences(context.getString(R.string.sp),1);
        this.lastIp= sp.getString("lastIp","");
    }


    //检测当前设备是否已经登陆且是否设备已满
    private boolean isLogin() throws IOException, JSONException {
        boolean flag=false;
        URL url=new URL("https://wifi.loocha.cn/"+id+"/wifi/status");
        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization",author);
        if(conn.getResponseCode()==200){
            BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream())) ;
            String result="";
            String line=null;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            JSONObject json=new JSONObject(result);
            if(!json.isNull("wifiOnlines")){
                JSONArray onlines=json
                        .getJSONObject("wifiOnlines")
                        .getJSONArray("onlines");
                int i=0;
                for(;i<onlines.length();i++){
                    JSONObject o=onlines.getJSONObject(i);
                    if(lastIp.equals(o.getString("brasIp")))
                        flag=true;
                }
                if(i==3) isFull=true;
            }

        }
        return flag;
    }
    //生成author,获取id,serverId


    //获取id,serverId
    private String login() throws IOException, JSONException {
        URL url=new URL("https://www.loocha.com.cn:8443/login");
        HttpURLConnection connection=(HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", author);
        switch(connection.getResponseCode()){
            case 200:
                BufferedReader in=new BufferedReader(new InputStreamReader(connection.getInputStream())) ;
                String result="";
                String line=null;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
                JSONObject json=new JSONObject(result);
                int status=json.getInt("status");
                if(status==0){
                    id=json.getJSONObject("user").getString("id");
                    serverId=json.getJSONObject("user").getString("did").replaceFirst("#.*", "");
                    return "0";
                }else{
                    return "login异常";
                }
            case 401:
                return"账号或密码错误";
        }
        return "login异常";
    }
    private String getQRCode() throws IOException, JSONException {
        URL url=new URL("https://wifi.loocha.cn/0/wifi/qrcode" + "?brasip="+brasIp + "&ulanip=" +wanIp + "&wlanip=" +wanIp);
        HttpURLConnection connection=(HttpURLConnection) url.openConnection();
        BufferedReader in=new BufferedReader(new InputStreamReader(connection.getInputStream())) ;
        String result="";
        String line=null;
        while ((line = in.readLine()) != null) {
            result += line;
        }

        JSONObject json=new JSONObject(result);

        int status=json.getInt("status");
        if(status==0){

            String qrcode= json.getJSONObject("telecomWifiRes").getString("password");

            return qrcode;
        }else{
            return "";
        }
    }

    //获取密码
    private String getPasswd() throws IOException, JSONException {
        URL url=new URL("https://wifi.loocha.cn/"+id+"/wifi?server_did="+serverId);
        HttpURLConnection connection=(HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", author);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        if(connection.getResponseCode()==200){
            String result="";
            String line=null;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            JSONObject json=new JSONObject(result);
            JSONObject telecomWifiRes=json.getJSONObject("telecomWifiRes");
            if(telecomWifiRes.isNull("password")) return "获取密码失败，请在掌上大学重新获取密码后尝试";
            return  telecomWifiRes.getString("password");
        }
    return "密码获取失败";

    }

    //获取ip,brasip
    private String getIp() throws IOException {
        URL url=new URL("http://test.f-young.cn");
        HttpURLConnection conn=(HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        if(conn.getResponseCode()==302){
            String param=conn.getHeaderField("Location").split("\\?")[1];
            String []args=param.split("&");
            wanIp=args[0].split("=")[1];
            brasIp=args[1].split("=")[1];
            return "0";
        }
        return "未能获取设备ip,请检查是否连接至校园网，或已登录";
    }

    //拨号
    private String dialUp(String qrCode,String code) throws IOException, JSONException {
        URL realUrl = new URL("https://wifi.loocha.cn/"+id+"/wifi/enable?qrcode="+qrCode+ "&code="+code);
        HttpsURLConnection conn = (HttpsURLConnection) realUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", author);
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        if(conn.getResponseCode()==200){
            BufferedReader in = null;
            String result = "";
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            JSONObject json=new JSONObject(result);
            int status=json.getInt("status");
            if(status==0){
                SharedPreferences sp=context.getSharedPreferences(context.getString(R.string.sp),1);
                SharedPreferences.Editor ed=sp.edit();
                ed.putString("lastIp",wanIp);
                ed.putString("brasIp",brasIp);
                ed.commit();
                this.lastIp= sp.getString("lastIp","");
                return "登陆成功";
            }
            else{
                return json.getString("response");
            }
        }
        return "登录失败";

    }

    private String kickOff() throws IOException {
        SharedPreferences sp=context.getSharedPreferences(context.getString(R.string.sp),1);
        wanIp=sp.getString("lastIp","");
        brasIp=sp.getString("brasIp","");
        if(wanIp.equals("")||brasIp.equals("")) return "尚未登陆";
        URL url=new URL("https://wifi.loocha.cn/"+id+"/wifi/kickoff?wanip="+wanIp+"&brasip="+brasIp);
        HttpURLConnection conn=(HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", author);
        conn.setRequestMethod("DELETE");
        int code=conn.getResponseCode();
        if(code==200){
            return "下线成功";
        }
        return "下线失败";
    }

    public void online(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        // TODO Auto-generated method stub
                        return true;
                    }
                });
                String info="";
                String ap=account+":"+passwd;
                author=Base64.encodeToString(ap.getBytes(),Base64.DEFAULT);
                author="Basic "+author;
                try {
                    info=login();
                    if(!info.equals("0"))return;
                    if(isLogin()){info="已经登陆";return;}
                    if(isFull){ info="设备已满";return;}
                    info=getIp();
                    if(!info.equals("0"))return;
//                    for(int i=0;i<3;i++){
                        String qrCode=getQRCode();

                        if(qrCode.equals("")){ info="code获取异常";return;}
                        info=getPasswd();

                        if(!info.matches("\\d{6}")) return;
                        String code=info;
                        info=dialUp(qrCode,code);
//                    }
                } catch (IOException e) {
                    info="网络访问异常";
                    e.printStackTrace();
                } catch (JSONException e) {
                    info="数据解析异常";
                    e.printStackTrace();
                }finally {
                    if(handler!=null){
                        Message m=new Message();
                        m.what=1;
                        m.obj=info;
                        handler.sendMessage(m);
                    }

                }
            }
        }).start();


    }

    public void offline(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String info = "";
                String ap=account+":"+passwd;
                author=Base64.encodeToString(ap.getBytes(),Base64.DEFAULT);
                author="Basic "+author;
                try {
                    info=login();
                    if(!info.equals("0"))return;
                    info=kickOff();
                } catch (IOException e) {
                    info="网络访问异常";
                    e.printStackTrace();
                } catch (JSONException e) {
                    info="数据解析异常";
                    e.printStackTrace();
                }finally {
                    if(handler!=null){
                        Message m=new Message();
                        m.what=1;
                        m.obj=info;
                        handler.sendMessage(m);
                    }
                }
            }
        }).start();

    }


}
