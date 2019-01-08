package com.mtr.oss.aliyun;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * 阿里云对象储存工具包<br>
 *     相关依赖
 *     <dependency>
 * 			<groupId>com.aliyun.oss</groupId>
 *			<artifactId>aliyun-sdk-oss</artifactId>
 *			<version>2.8.3</version>
 * 		</dependency>
 * @Title: AliyunOssUtil
 * @author mtr
 * @date 2019/1/6  16:23
 */
@Configuration
public class AliyunOssUtil {

    @Value("${oss.aliyun.endpoint}")
    private String endpoint;//阿里云oss EndPoint（地域节点）eg:http://oss-cn-qingdao.aliyuncs.com

    @Value("${oss.aliyun.accessKeyId}")
    private String accessKeyId;

    @Value("${oss.aliyun.accessKeySecret}")
    private String accessKeySecret;

    @Value("${oss.aliyun.bucketName}")
    private String bucketName;//阿里云oss bucket名称 即储存空间名称

    public String getEndpoint() {
        return endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    //工程中可以有一个或多个OSSClient 用于创建OSSClient
    private ClientConfiguration conf;

    //OSSClient实例
    private OSSClient ossClient;

    /**
     * 把工具加入到spring容器中
     * @param
     * @Return com.mtr.oss.aliyun.AliyunOssUtil
     * @Exception
     * @Author mtr
     * @Date 2019/1/6 16:26
     */
    //@Bean
    public AliyunOssUtil getAliyunOss(){
        return new AliyunOssUtil();
    }

    /**
     * 获取一个OSSClient实例 用完后需要自己关闭 ossClient.shutdown();方法
     * @param 
     * @return com.aliyun.oss.OSSClient
     * @exception 
     * @author mtr
     * @date 2019/1/7 13:57
     */ 
    public OSSClient getOssClient(){
        return new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
    }

    /**
     * 通过无参构造创建ClientConfiguration实例
     * @param
     * @Return
     * @Exception
     * @Author mtr
     * @Date
     */
    public AliyunOssUtil() {
        // 创建ClientConfiguration实例，按照您的需要修改默认参数。
        conf = new ClientConfiguration();
        // 开启支持CNAME。CNAME是指将自定义域名绑定到存储空间上。
        conf.setSupportCname(true);
    }

    /**
     * 流文件上传 适合照片,短音频等小文件
     *   把文件通过流存储到阿里云OSS中
     * @param paperName 储存到阿里云OSS后的文件路径 e.g image/
     * @param fileName  储存到OSS后的文件文件名 e.g a.txt
     * @param is    要储存的文件流
     * @return  java.lang.String
     * @exception
     * @author mtr
     * @date 2019/1/6 16:28
     */
    public String uploadStream(String paperName, String fileName, InputStream is){
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        String objectName = paperName + fileName;
        //开始上传文件
        ossClient.putObject(bucketName, objectName, is);
        // 关闭OSSClient。
        ossClient.shutdown();
        return objectName;
    }

    /**
     * 判断文件是否存在
     * @param objectName 储存的文件路径加文件名 <br> eg: demo/demo.jpg
     * @return boolean
     * @exception 
     * @author mtr
     * @date 2019/1/6 17:13
     */ 
    public boolean fileIsExist(String objectName){
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        // 判断文件是否存在
        boolean found = ossClient.doesObjectExist(bucketName, objectName);
        // 关闭OSSClient。
        ossClient.shutdown();
        return found;
    }

    /**
     * 删除单个文件
     * @param objectName    要删除的文件路径加文件名 <br> eg: demo/demo.jpg
     * @return boolean  返回false表示文件不存在   返回true表示删除成功
     * @exception
     * @author mtr
     * @date 2019/1/6 17:32
     */
    public boolean deleteFileOne(String objectName){
        //先判断文件是否存在
        boolean found = fileIsExist(objectName);
        if(!found){
            //文件不存在
            return false;
        }
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        //删除文件
        ossClient.deleteObject(bucketName, objectName);
        return true;
    }

    /** 用于设置文件权限为私有*/
    public static final CannedAccessControlList PRIVATE = CannedAccessControlList.Private;
    /** 用于设置文件权限为公共可读*/
    public static final CannedAccessControlList PUBLIC_READ = CannedAccessControlList.PublicRead;
    /** 用于设置文件权限为公共可读写 慎用*/
    public static final CannedAccessControlList PUBLIC_READ_WRITE = CannedAccessControlList.PublicReadWrite;
    /** 用于设置文件权限为公共可读写 慎用*/
    public static final CannedAccessControlList DEFAULT = CannedAccessControlList.Default;

    /**
     * 设置文件权限
     * @param objectName    文件路径加文件名 <br> eg: demo/demo.jpg
     * @param acl   文件权限
     * @return java.lang.String 返回文件权限
     * @exception
     * @author mtr
     * @date 2019/1/6 18:44
     */
    public String fileAcl(String objectName, CannedAccessControlList acl){
        //先判断文件是否存在
        if(!fileIsExist(objectName)){
            //文件不存在
            return "文件不存在";
        }
        if (acl == null) {
            return "权限为空";
        }
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        // 设置文件的访问权限为公共读。
        ossClient.setObjectAcl(bucketName, objectName, acl);
        // 关闭OSSClient。
        ossClient.shutdown();
        switch (acl){
            case Private: return "private";
            case PublicRead: return "public-read";
            case PublicReadWrite: return "public-read-write";
            case Default: return "default";
        }
        return "失败";
    }

    /**
     * 获取文件权限
     * @param objectName 文件路径加文件名 <br> eg: demo/demo.jpg
     * @return java.lang.String 若文件不存在返回"文件不存在",如文件存在返回对应的权限
     * @exception 
     * @author mtr
     * @date 2019/1/6 18:43
     */ 
    public String fileAcl(String objectName){
        //先判断文件是否存在
        if(!fileIsExist(objectName)){
            //文件不存在
            return "文件不存在";
        }
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        // 获取文件的访问权限。
        ObjectAcl objectAcl = ossClient.getObjectAcl(bucketName, objectName);
        // 关闭OSSClient。
        ossClient.shutdown();
        return objectAcl.getPermission().toString();
    }

    /**
     * 获取一个文件一定时间内的访问连接
     * @param objectName    文件路径加文件名 <br> eg: demo/demo.jpg
     * @param time  可访问的时间 秒数
     * @return java.lang.String 返回连接
     * @exception 
     * @author mtr
     * @date 2019/1/6 19:55
     */ 
    public String getUrl(String objectName, Long time){
        //先判断文件是否存在
        if(!fileIsExist(objectName)){
            //文件不存在
            return "文件不存在";
        }
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        // 设置URL过期时间为1小时。
        Date expiration = new Date(new Date().getTime() + time * 1000);
        // 生成以GET方法访问的签名URL，访客可以直接通过浏览器访问相关内容。
        URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
        // 关闭OSSClient。
        ossClient.shutdown();
        return url.toString();
    }

    /**
     * 用于下载小文件  个人建议不大于10M  大于10M直接在Controller中处理
     * @param objectName  文件路径加文件名 <br> eg: demo/demo.jpg
     * @return byte[]   返回的是要下载的文件数据,
     * @throws 
     * @author mtr
     * @date 2019/1/7 21:38
     */ 
    public byte[] downloadStream(String objectName) throws IOException {
        //先判断文件是否存在
        if(!fileIsExist(objectName)){
            //文件不存在
            return null;
        }
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        // ossObject包含文件所在的存储空间名称、文件名称、文件元信息以及一个输入流。
        OSSObject ossObject = ossClient.getObject(bucketName, objectName);
        InputStream inputStream = ossObject.getObjectContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //从输入流中获取数据
        byte[] buffer = new byte[1024];
        int len = 0;
        while( (len=inputStream.read(buffer)) != -1 ){
            //将数据写到浏览器
            baos.write(buffer, 0, len);
        }
        //关闭文件流
        inputStream.close();
        byte[] file = baos.toByteArray();
        // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
        ossObject.close();
        // 关闭OSSClient。
        ossClient.shutdown();
        return file;
    }

    /**
     *
     * @param prefix    文件夹名,最后带 /  前面不带
     * @return java.util.List<java.lang.String> 不传值代表查找空间下所有文件
     * @throws 
     * @author mtr
     * @date 2019/1/7 21:21
     */ 
    /**
     * 分页获取文件夹内文件夹内文件
     * @param prefix    文件夹名,最后带 /  前面不带
     * @param maxNum    一页最多显示的数据条数 填null表示100条
     * @param marker    从哪里开始显示
     * @return java.util.List<java.lang.String> prefix 不传值代表查找空间下所有文件
     * @throws 
     * @author zhaoruirui
     * @date 2019/1/7 22:28
     */ 
    public List<String> objectNameListByPrefix(String prefix, Integer maxNum, String marker){
        List<String> objectNameList = new ArrayList<>();
        // 创建OSSClient实例。
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret, conf);
        // 构造ListObjectsRequest请求。
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
        // 设置prefix参数来获取fun目录下的所有文件。或者以prefix开头的文件
        listObjectsRequest.withPrefix(prefix);
        // 指定每页最大文件个数。
        listObjectsRequest.withMaxKeys(maxNum != null ? maxNum : 100);
        // 列举指定marker之后的文件。
        listObjectsRequest.withMarker(marker);
        // 递归列出fun目录下的所有文件。
        ObjectListing listing = ossClient.listObjects(listObjectsRequest);
        // 遍历所有文件。
        System.out.println("---------------Objects:");
        for (OSSObjectSummary objectSummary : listing.getObjectSummaries()) {
            System.out.println("文件名 : " + objectSummary.getKey());
            System.out.println("文件所属空间 : " + objectSummary.getBucketName());
            System.out.println("文件最后一次修改时间 : " + objectSummary.getLastModified());
            System.out.println("文件大小 : " + objectSummary.getSize());
            System.out.println("---------------------------------------------------");
            //将文件名放到集合中 可以封装对象 或者 map
            objectNameList.add(objectSummary.getKey());
        }
        // 关闭OSSClient。
        ossClient.shutdown();
        return objectNameList;
    }


}
