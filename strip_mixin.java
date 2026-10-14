import java.io.*;
import java.util.*;
import java.util.zip.*;

public class strip_mixin {
    public static void main(String[] args) throws IOException {
        for (String jarPath : args) {
            File jarFile = new File(jarPath);
            File tempFile = new File(jarPath + ".tmp");
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile));
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
                
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.getName().startsWith("org/spongepowered/asm/mixin")) {
                        zos.putNextEntry(new ZipEntry(entry.getName()));
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                    zis.closeEntry();
                }
            }
            
            // Replace original with stripped version
            java.nio.file.Files.move(tempFile.toPath(), jarFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Stripped: " + jarPath);
        }
    }
}
