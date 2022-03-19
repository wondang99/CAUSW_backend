package net.causw.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import net.causw.domain.model.ImageLocation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static net.causw.domain.model.StaticValue.GCS_PUBLIC_LINK_PREFIX;

@Component
public class GcpFileUploader {
    @Value("${spring.cloud.gcp.credentials.location}")
    private String KEY_FILE;

    @Value("${spring.cloud.gcp.project-id}")
    private String PROJECT_ID;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String BUCKET_NAME;

    public String uploadFileToGcp(MultipartFile file) {
        try {
            InputStream keyFile = ResourceUtils.getURL(KEY_FILE).openStream();

            Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID)
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build().getService();

            String path = "ATTACHMENTS"
                    + "/"
                    + DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSSZ").format(ZonedDateTime.now())
                    + "/"
                    + file.getOriginalFilename();

            storage.create(
                    BlobInfo.newBuilder(BUCKET_NAME, path)
                            .build(),
                    file.getBytes()
            );

            return GCS_PUBLIC_LINK_PREFIX + BUCKET_NAME + "/" + path;
        } catch(IllegalStateException | IOException e){
            throw new RuntimeException(e);
        }
    }

    public String uploadImageToGcp(MultipartFile image, ImageLocation imageLocation) {
        try {
            InputStream keyFile = ResourceUtils.getURL(KEY_FILE).openStream();

            Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID)
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build().getService();

            String path = Optional.ofNullable(imageLocation).orElse(ImageLocation.ETC)
                    + "/"
                    + DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now())
                    + "/"
                    + DateTimeFormatter.ofPattern("HH-mm-ss-SSSZ").format(ZonedDateTime.now())
                    + "_"
                    + image.getOriginalFilename();

            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder(BUCKET_NAME, path)
                            .build(),
                    image.getBytes()
            );

            return blobInfo.getMediaLink();
        } catch(IllegalStateException | IOException e){
            throw new RuntimeException(e);
        }
    }
}
