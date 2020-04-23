package org.thoughtcrime.securesms.profiles;


import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AvatarHelper {

    public static void setGroupAvatar(Context context, int chatId, Bitmap bitmap) {
        DcContext dcContext = DcHelper.getContext(context);

        if (bitmap == null) {
            dcContext.setChatProfileImage(chatId, null);
        } else {
            try {
                File avatar = File.createTempFile("groupavatar", ".jpg", context.getCacheDir());
                FileOutputStream out = new FileOutputStream(avatar);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                out.close();
                dcContext.setChatProfileImage(chatId, avatar.getPath()); // The avatar is copied to the blobs directory here...
                //noinspection ResultOfMethodCallIgnored
                avatar.delete(); // ..., now we can delete it.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File getSelfAvatarFile(@NonNull Context context) {
        String dirString = DcHelper.getContext(context).getConfig(DcHelper.CONFIG_SELF_AVATAR);
        return new File(dirString);
    }

    public static void setSelfAvatar(@NonNull Context context, @Nullable byte[] data) throws IOException {
        if (data == null) {
            DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, null);
        } else {
            File avatar = File.createTempFile("selfavatar", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(avatar);
            out.write(data);
            out.close();
            DcHelper.set(context, DcHelper.CONFIG_SELF_AVATAR, avatar.getPath()); // The avatar is copied to the blobs directory here...
            //noinspection ResultOfMethodCallIgnored
            avatar.delete(); // ..., now we can delete it.
        }
    }
}
