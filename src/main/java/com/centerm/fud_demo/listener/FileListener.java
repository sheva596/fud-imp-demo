package com.centerm.fud_demo.listener;import com.centerm.fud_demo.constant.Constants;import com.centerm.fud_demo.dao.FileDao;import com.centerm.fud_demo.entity.FileRecord;import com.centerm.fud_demo.service.BackupService;import com.centerm.fud_demo.utils.FileUtil;import lombok.extern.slf4j.Slf4j;import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;import org.apache.commons.io.monitor.FileAlterationObserver;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import java.io.File;/** * @author Sheva * @version 1.0 * @date 2020/2/15 上午11:01 */@Component@Slf4jpublic class FileListener extends FileAlterationListenerAdaptor {    @Value("${filePath}")    private String filePath;    @Value("${backupPath}")    private String backupPath;    @Autowired    private FileDao fileDao;    @Autowired    private BackupService backupService;    @Override    public void onStart(FileAlterationObserver observer) {        log.info("File Listener start...");    }    @Override    public void onFileCreate(File file) {        String fileName = file.getName();        if (file.renameTo(file)){            String finalName = fileName.substring(0, fileName.lastIndexOf("."));            String fileSize = FileUtil.getFormatSize(file.length());            String fileMd5 = FileUtil.getFileMd5(file.getPath());            String suffix = fileName.substring(fileName.lastIndexOf("."));            String filePath = file.getPath().replaceAll("\\\\", "/");            log.info("添加的文件地址为： " + filePath);            Long fileId = fileDao.getFileIdByUrl(filePath);            FileRecord temp = fileDao.getFileById(fileId);            if (fileId != null){                log.info("File exists in database...");                backupService.backupFile(filePath, backupPath + temp.getMd5(), fileName);                return;            }            FileRecord fileRecord = new FileRecord(finalName, filePath, fileSize, Constants.SUPERVIPID, fileMd5, suffix, backupPath + fileMd5);            fileDao.addFileRecord(fileRecord);            backupService.backupFile(filePath, backupPath + fileMd5, fileName);            log.info("file added into database...");        }        log.info("(onFileCreate)File created: " + fileName);    }    @Override    public void onFileDelete(File file) {        String filePath = file.getPath().replaceAll("\\\\", "/");        String backupUrl = backupService.getFileBackupPath(filePath);        if (null != backupUrl){        log.info("文件备份地址为：" + backupUrl);        FileUtil.deleteDirectory(backupUrl);        fileDao.deleteFileByPath(filePath);        log.info("File deleted: " + filePath);        }else {            log.info("file is already deleted...");        }    }    @Override    public void onFileChange(File file) {        String fileName = file.getName();        if (file.renameTo(file)){            String filePath = file.getPath().replaceAll("\\\\", "/");            String fileMd5 = FileUtil.getFileMd5(filePath);            String fileSize = FileUtil.getFormatSize(file.length());            Long fileId = fileDao.getFileIdByUrl(filePath);            String backupUrl = backupService.getFileBackupPath(filePath);            fileDao.updateFileRecord(fileId, fileSize, fileMd5, backupPath + fileMd5);            FileUtil.deleteDirectory(backupUrl);            backupService.backupFile(filePath, backupPath + fileMd5, fileName);            log.info("(onFileChange)file update successfully...");        }        log.info("File changed: " + file.getName());    }    @Override    public void onStop(FileAlterationObserver observer) {        log.info("File Listener stop...");    }}