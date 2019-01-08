package com.mtr.oss.controller;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;
import com.mtr.oss.aliyun.AliyunOssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云OSS上传
 * @author mtr
 * @title: AliyunController
 * @projectName oss
 * @date 2019/1/5  17:52
 */
@RestController
@RequestMapping("/aliyun")
public class AliyunController {

    @Autowired
    private AliyunOssUtil aliyunOssUtil;

    /**
     * 用于文件上传
     * @param file 浏览器上传过来的文件
     * @return java.util.Map<java.lang.String,java.lang.Object> 储存返回客户端的信息
     * @exception
     * @author mtr
     * @date 2019/1/6 16:50
     */
    @PostMapping("/streamFile")
    public Map<String, Object> uploadFileStream(@RequestParam("file") MultipartFile file) throws IOException {
        //用于计时
        StopWatch sw = new StopWatch();
        //计时开时
        sw.start();
        System.out.println("--开始上传");
        Map<String,Object> data = new HashMap<>();
        if (file.isEmpty()) {
            //文件为空
            data.put("msg","文件为空");
            return data;
        }
        InputStream is = file.getInputStream();
        String paperName = "test/";
        //获取原文件名
        String fileName = file.getOriginalFilename();
        //获取原文件后缀
        fileName = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        //生成新文件名
        fileName = UUID.randomUUID().toString() + fileName;
        //开始上传文件
        String path = aliyunOssUtil.uploadStream(paperName, fileName, is);
        data.put("msg","成功");
        data.put("data",path);
        //计时结束
        sw.stop();
        System.out.println("--文件上传成功:fileName = " + fileName);
        System.out.println("---上传耗时间：" + sw.getTotalTimeSeconds() + "s");
        return data;
    }

    /**
     * 删除文件
     * @param objectName 浏览器传来的问件名
     * @return java.util.Map<java.lang.String,java.lang.Object>
     * @exception
     * @author mtr
     * @date 2019/1/6 17:38
     */
    @GetMapping("/deleteOne")
    public Map<String, Object> deleteFile(String objectName){
        Map<String,Object> data = new HashMap<>();
        boolean b = aliyunOssUtil.deleteFileOne(objectName);
        if(!b){
            data.put("msg","文件不存在");
            return data;
        }
        data.put("msg","成功");
        return data;
    }

    /**
     * acl传值为设置文件权限,不传值为获取文件权限
     * @param objectName    文件名带路径
     * @param acl   文件权限
     * @return java.util.Map<java.lang.String,java.lang.Object>
     * @exception
     * @author mtr
     * @date 2019/1/6 18:59
     */
    @GetMapping("/acl")
    public  Map<String, Object> fileACL(String objectName,String acl){
        Map<String,Object> data = new HashMap<>();
        String fileAcl = null;
        if (StringUtils.isEmpty(acl)) {
            //获取文件可读性
            fileAcl = aliyunOssUtil.fileAcl(objectName);
            data.put("msg",fileAcl);
            return data;
        }
        switch (acl){
            case "private": //私有
                fileAcl = aliyunOssUtil.fileAcl(objectName, AliyunOssUtil.PRIVATE);
                break;
            case "publicR": //公共可读
                fileAcl = aliyunOssUtil.fileAcl(objectName, AliyunOssUtil.PUBLIC_READ);
                break;
            case "publicRW": //公共读写 慎用
                fileAcl = aliyunOssUtil.fileAcl(objectName, AliyunOssUtil.PUBLIC_READ_WRITE);
                break;
            case "default": //默认
                fileAcl = aliyunOssUtil.fileAcl(objectName, AliyunOssUtil.DEFAULT);
                break;
            default:
                fileAcl = "权限无效";
                break;
        }
        data.put("msg",fileAcl);
        return data;
    }

    /**
     * 获取一个文件一定时间内的访问连接
     * @param objectName 文件名带路径
     * @return java.util.Map<java.lang.String,java.lang.Object>
     * @exception
     * @author mtr
     * @date 2019/1/6 19:59
     */
    @GetMapping("/url")
    public Map<String, Object> getUrl(String objectName){
        Map<String,Object> data = new HashMap<>();
        String url = aliyunOssUtil.getUrl(objectName, 10L);
        data.put("msg",url);
        return data;
    }

    @GetMapping("/down")
    public void dFile(@RequestParam("objectName") String objectName, HttpServletResponse response){
        //用于计时
        StopWatch sw = new StopWatch();
        //计时开始
        sw.start();
        System.out.println("--开始下载");
        String[] on = objectName.split("/");
        //最终下载的文件名
        String fileName = on[on.length-1];
        //判断是否传值了
        if(!StringUtils.isEmpty(objectName)){
            InputStream inputStream = null;
            try {
                OSSClient ossClient = aliyunOssUtil.getOssClient();
                // ossObject包含文件所在的存储空间名称、文件名称、文件元信息以及一个输入流。
                OSSObject ossObject = ossClient.getObject(aliyunOssUtil.getBucketName(), objectName);
                //获取文件流
                inputStream = ossObject.getObjectContent();
                if (inputStream != null) {
                    // 设置强制下载不打开
                    response.setContentType("application/force-download");
                    //response.setContentType("application/octet-stream");
                    //文件名
                    response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
                    //response.addHeader("Content-Length", "" + dataStream.length);
                    OutputStream outputStream = response.getOutputStream();
                    //从输入流中获取数据
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while( (len=inputStream.read(buffer)) != -1 ){
                        //将数据写到浏览器
                        outputStream.write(buffer, 0, len);
                    }
                    //关闭文件流
                    inputStream.close();
                    //刷新网络流
                    outputStream.flush();
                    //关闭网络流
                    outputStream.close();
                    System.out.println("---下载成功");
                    // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
                    ossObject.close();
                    // 关闭OSSClient。
                    ossClient.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //计时结束
        sw.stop();
        System.out.println("--文件下载成功:fileName = " + fileName);
        System.out.println("---下载耗时间：" + sw.getTotalTimeSeconds() + "s");
    }

    /**
     * 用于获取文件夹内所有数据
     * @param objectName    文件夹名
     * @return java.util.Map<java.lang.String,java.lang.Object>
     * @throws 
     * @author mtr
     * @date 2019/1/7 21:43
     */
    @GetMapping("/objectList")
    public Map<String,Object> getObjectNameList(String objectName) {
        Map<String, Object> data = new HashMap<>();
        List<String> objectNameList = aliyunOssUtil.objectNameListByPrefix(objectName,10,null);
        data.put("msg", "成功");
        data.put("data", objectNameList);
        return data;
    }

    /**
     * 用来处理当前类中的异常
     * @param ex  捕获到的异常信息
     * @return java.util.Map<java.lang.String,java.lang.Object> 储存返回客户端的信息
     * @exception
     * @author mtr
     * @date 2019/1/6 16:45
     */
    @ExceptionHandler
    public Map<String, Object> error(Exception ex){
        ex.printStackTrace();
        Map<String,Object> data = new HashMap<>();
        data.put("msg","服务器异常");
        return data;
    }


}
