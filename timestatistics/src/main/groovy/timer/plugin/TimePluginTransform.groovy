package timer.plugin;

import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter;

class TimePluginTransform extends Transform {
    @Override
    public String getName() {
        return TimePluginTransform.class.getSimpleName()
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 指Transform要操作内容的范围，官方文档Scope有7种类型：
     * <p>
     * EXTERNAL_LIBRARIES        只有外部库
     * PROJECT                   只有项目内容
     * PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
     * PROVIDED_ONLY             只提供本地或远程依赖项
     * SUB_PROJECTS              只有子项目。
     * SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * TESTED_CODE               由当前变量(包括依赖项)测试的代码
     * SCOPE_FULL_PROJECT        整个项目
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        println 'transform -------'
         //inputs 为传入的数据流，一种是jar格式，一种是目录格式

        Collection<TransformInput> inputs = transformInvocation.getInputs();
        // 获取输出目录，并将修改后的文件复制到输出目录
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()

        for (TransformInput input : inputs) {
            // 处理jar 包中的class 文件

            for (JarInput jarInput : input.getJarInputs()) {
                File dest = outputProvider.getContentLocation(
                        jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR
                )
                FileUtils.copyFile(jarInput.getFile(), dest)
            }

            // 处理文件目录下的class 文件
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                handleDirectoryInput(directoryInput, outputProvider)
            }

        }
    }

    // 临时文件
    private List<File> mTempFiles = new ArrayList<>()

    private void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) throws IOException {
        // 列出目录所有文件（包含子文件夹，子文件夹内文件）
        File dir = directoryInput.getFile();

        // 判断是否为目录
        if (directoryInput.getFile().isDirectory()) {
            // 查找目录下面所有的文件
            mTempFiles.clear();
            getDirFiles(dir);
            // 遍历所有文件
            for (File file : mTempFiles) {
                // 处理相应文件
                tranformFile(file);
            }
        }
        // 判断是否为文件
        else if (dir.isFile()) {
            // 处理相应文件
            tranformFile(dir);
        } else {
            return;
        }
        // Transform 拷贝文件到 transforms 目录
        File dest = outputProvider.getContentLocation(
                directoryInput.getName(),
                directoryInput.getContentTypes(),
                directoryInput.getScopes(),
                Format.DIRECTORY);
        // 将修改过的字节码copy到dest，实现编译期间干预字节码
        FileUtils.copyDirectory(directoryInput.getFile(), dest);
    }


    // 遍历查找所有的文件
    private void getDirFiles(File dir) {
        File[] files = dir.listFiles()
        for (File file : files) {
            if (file.isDirectory()) {
                getDirFiles(file)
            } else if (file.isFile()) {
                mTempFiles.add(file)
            }
        }
    }

    // 处理响应的文件
    private void tranformFile(File file) throws IOException {
        String fileName = file.getName()
        if (filerClass(fileName)) {
            println "check pass" + fileName
            FileInputStream fis = new FileInputStream(file)
            ClassReader classReader = new ClassReader(fis)
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new TimePluginClassVisitor(classWriter)
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            byte[] bytes = classWriter.toByteArray()

            // 改写class 字节码
            FileOutputStream outputStream
            try {
                outputStream = new FileOutputStream(file.getPath())
                //写入流
                outputStream.write(bytes)
                outputStream.close()
            } catch (IOException e) {
            }
        }
    }

    private boolean filerClass(String name) {
        return name.endsWith("Activity.class")
    }
}
