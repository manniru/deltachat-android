/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ImageReceiver;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.UserObject;
import com.b44t.messenger.R;
import com.b44t.messenger.ConnectionsManager;
import com.b44t.messenger.TLObject;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.UserConfig;
import com.b44t.ui.Components.AvatarDrawable;
import com.b44t.ui.ActionBar.Theme;

public class ProfileSearchCell extends BaseCell {

    private static TextPaint namePaint;
    private static TextPaint onlinePaint;
    private static TextPaint offlinePaint;
    private static TextPaint countPaint;
    private static Drawable groupDrawable;
    private static Drawable countDrawable;
    private static Drawable countDrawableGrey;
    private static Paint linePaint;

    private CharSequence currentName;
    private ImageReceiver avatarImage;
    private AvatarDrawable avatarDrawable;
    private CharSequence subLabel;

    private TLRPC.User user = null;
    private TLRPC.Chat chat = null;
    private final Object encryptedChat = null;
    private long dialog_id;

    private String lastName = null;
    private int lastStatus = 0;
    private TLRPC.FileLocation lastAvatar = null;

    public boolean useSeparator = false;
    public float drawAlpha = 1;

    private int nameLeft;
    private int nameTop;
    private StaticLayout nameLayout;
    private boolean drawNameGroup;
    private int nameLockLeft;
    private int nameLockTop;

    private boolean drawCount;
    private int lastUnreadCount;
    private int countTop = AndroidUtilities.dp(25);
    private int countLeft;
    private int countWidth;
    private StaticLayout countLayout;

    private int onlineLeft;
    private StaticLayout onlineLayout;

    public ProfileSearchCell(Context context) {
        super(context);

        if (namePaint == null) {
            namePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            namePaint.setTextSize(AndroidUtilities.dp(17));
            namePaint.setColor(0xff212121);
            namePaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

            onlinePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            onlinePaint.setTextSize(AndroidUtilities.dp(16));
            onlinePaint.setColor(Theme.MSG_LINK_TEXT_COLOR);

            offlinePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            offlinePaint.setTextSize(AndroidUtilities.dp(16));
            offlinePaint.setColor(0xff999999);

            linePaint = new Paint();
            linePaint.setColor(0xffdcdcdc);

            countPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            countPaint.setTextSize(AndroidUtilities.dp(13));
            countPaint.setColor(0xffffffff);
            countPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

            groupDrawable = getResources().getDrawable(R.drawable.list_group);
            countDrawable = getResources().getDrawable(R.drawable.dialogs_badge);
            countDrawableGrey = getResources().getDrawable(R.drawable.dialogs_badge2);
        }

        avatarImage = new ImageReceiver(this);
        avatarImage.setRoundRadius(AndroidUtilities.dp(26));
        avatarDrawable = new AvatarDrawable();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= 21 && getBackground() != null) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                getBackground().setHotspot(event.getX(), event.getY());
            }
        }
        return super.onTouchEvent(event);
    }

    public void setData(TLObject object, Object ec, CharSequence n, CharSequence s, boolean needCount) {
        currentName = n;
        if (object instanceof TLRPC.User) {
            user = (TLRPC.User) object;
            chat = null;
        } else if (object instanceof TLRPC.Chat) {
            chat = (TLRPC.Chat) object;
            user = null;
        }
        subLabel = s;
        drawCount = needCount;
        update(0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(72));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (user == null && chat == null && encryptedChat == null) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        if (changed) {
            buildLayout();
        }
    }

    public void buildLayout() {
        CharSequence nameString;
        TextPaint currentNamePaint;

        drawNameGroup = false;

        {
            if (chat != null) {
                /*if (chat.id < 0) {
                    dialog_id = AndroidUtilities.makeBroadcastId(chat.id);
                    drawNameBroadcast = true;
                    nameLockTop = AndroidUtilities.dp(28.5f);
                } else*/ {
                    dialog_id = -chat.id;
                    /*if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        drawNameBroadcast = true;
                        nameLockTop = AndroidUtilities.dp(28.5f);
                    } else*/ {
                        drawNameGroup = true;
                        nameLockTop = AndroidUtilities.dp(30);
                    }
                }
                if (!LocaleController.isRTL) {
                    nameLockLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
                    nameLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline + 4) + (groupDrawable.getIntrinsicWidth());
                } else {
                    nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline + 2) - (groupDrawable.getIntrinsicWidth());
                    nameLeft = AndroidUtilities.dp(11);
                }
            } else {
                dialog_id = user.id;
                if (!LocaleController.isRTL) {
                    nameLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
                } else {
                    nameLeft = AndroidUtilities.dp(11);
                }
                nameLockTop = AndroidUtilities.dp(17);
            }
        }

        if (currentName != null) {
            nameString = currentName;
        } else {
            String nameString2 = "";
            if (chat != null) {
                nameString2 = chat.title;
            } else if (user != null) {
                nameString2 = UserObject.getUserName(user);
            }
            nameString = nameString2.replace('\n', ' ');
        }

        currentNamePaint = namePaint;

        int onlineWidth;
        int nameWidth;
        if (!LocaleController.isRTL) {
            onlineWidth = nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(14);
        } else {
            onlineWidth = nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(AndroidUtilities.leftBaseline);
        }
        /*if (drawNameLock) {
            nameWidth -= AndroidUtilities.dp(6) + lockDrawable.getIntrinsicWidth();
        } else*/ if (drawNameGroup) {
            nameWidth -= AndroidUtilities.dp(6) + groupDrawable.getIntrinsicWidth();
        }

        if (drawCount) {
            TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
            if (dialog != null && dialog.unread_count != 0) {
                lastUnreadCount = dialog.unread_count;
                String countString = String.format("%d", dialog.unread_count);
                countWidth = Math.max(AndroidUtilities.dp(12), (int) Math.ceil(countPaint.measureText(countString)));
                countLayout = new StaticLayout(countString, countPaint, countWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                int w = countWidth + AndroidUtilities.dp(18);
                nameWidth -= w;
                if (!LocaleController.isRTL) {
                    countLeft = getMeasuredWidth() - countWidth - AndroidUtilities.dp(19);
                } else {
                    countLeft = AndroidUtilities.dp(19);
                    nameLeft += w;
                }
            } else {
                lastUnreadCount = 0;
                countLayout = null;
            }
        } else {
            lastUnreadCount = 0;
            countLayout = null;
        }

        CharSequence nameStringFinal = TextUtils.ellipsize(nameString, currentNamePaint, nameWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
        nameLayout = new StaticLayout(nameStringFinal, currentNamePaint, nameWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        if (chat == null || subLabel != null) {
            if (!LocaleController.isRTL) {
                onlineLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
            } else {
                onlineLeft = AndroidUtilities.dp(11);
            }

            CharSequence onlineString = "";
            TextPaint currentOnlinePaint = offlinePaint;

            if (subLabel != null) {
                onlineString = subLabel;
            } else if (user != null) {
                onlineString = "ErrOnline";
            }

            CharSequence onlineStringFinal = TextUtils.ellipsize(onlineString, currentOnlinePaint, onlineWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
            onlineLayout = new StaticLayout(onlineStringFinal, currentOnlinePaint, onlineWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            nameTop = AndroidUtilities.dp(13);
            if (subLabel != null && chat != null) {
                nameLockTop -= AndroidUtilities.dp(12);
            }
        } else {
            onlineLayout = null;
            nameTop = AndroidUtilities.dp(25);
        }

        int avatarLeft;
        if (!LocaleController.isRTL) {
            avatarLeft = AndroidUtilities.dp(AndroidUtilities.isTablet() ? 13 : 9);
        } else {
            avatarLeft = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.isTablet() ? 65 : 61);
        }

        avatarImage.setImageCoords(avatarLeft, AndroidUtilities.dp(10), AndroidUtilities.dp(52), AndroidUtilities.dp(52));

        double widthpx;
        float left;
        if (LocaleController.isRTL) {
            if (nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineLeft(0);
                if (left == 0) {
                    widthpx = Math.ceil(nameLayout.getLineWidth(0));
                    if (widthpx < nameWidth) {
                        nameLeft += (nameWidth - widthpx);
                    }
                }
            }
            if (onlineLayout != null && onlineLayout.getLineCount() > 0) {
                left = onlineLayout.getLineLeft(0);
                if (left == 0) {
                    widthpx = Math.ceil(onlineLayout.getLineWidth(0));
                    if (widthpx < onlineWidth) {
                        onlineLeft += (onlineWidth - widthpx);
                    }
                }
            }
        } else {
            if (nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineRight(0);
                if (left == nameWidth) {
                    widthpx = Math.ceil(nameLayout.getLineWidth(0));
                    if (widthpx < nameWidth) {
                        nameLeft -= (nameWidth - widthpx);
                    }
                }
            }
            if (onlineLayout != null && onlineLayout.getLineCount() > 0) {
                left = onlineLayout.getLineRight(0);
                if (left == onlineWidth) {
                    widthpx = Math.ceil(onlineLayout.getLineWidth(0));
                    if (widthpx < onlineWidth) {
                        onlineLeft -= (onlineWidth - widthpx);
                    }
                }
            }
        }
    }

    public void update(int mask) {
        TLRPC.FileLocation photo = null;
        if (user != null) {
            if (user.photo != null) {
                photo = user.photo.photo_small;
            }
            avatarDrawable.setInfoByUser(user);
        } else if (chat != null) {
            if (chat.photo != null) {
                photo = chat.photo.photo_small;
            }
            avatarDrawable.setInfoByChat(chat);
        } else {
            avatarDrawable.setInfoByName(null);
        }

        if (mask != 0) {
            boolean continueUpdate = false;
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 && user != null || (mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 && chat != null) {
                if (lastAvatar != null && photo == null || lastAvatar == null && photo != null && lastAvatar != null && photo != null && (lastAvatar.volume_id != photo.volume_id || lastAvatar.local_id != photo.local_id)) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_STATUS) != 0 && user != null) {
                int newStatus = 0;
                if (newStatus != lastStatus) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate && ((mask & MessagesController.UPDATE_MASK_NAME) != 0 && user != null) || (mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0 && chat != null) {
                String newName;
                if (user != null) {
                    newName = user.first_name + user.last_name;
                } else {
                    newName = chat.title;
                }
                if (!newName.equals(lastName)) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate && drawCount && (mask & MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE) != 0) {
                TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                if (dialog != null && dialog.unread_count != lastUnreadCount) {
                    continueUpdate = true;
                }
            }

            if (!continueUpdate) {
                return;
            }
        }

        if (user != null) {
            lastStatus = 0;
            lastName = user.first_name + user.last_name;
        } else if (chat != null) {
            lastName = chat.title;
        }


        lastAvatar = photo;
        avatarImage.setImage(photo, "50_50", avatarDrawable, null, false);

        if (getMeasuredWidth() != 0 || getMeasuredHeight() != 0) {
            buildLayout();
        } else {
            requestLayout();
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (user == null && chat == null && encryptedChat == null) {
            return;
        }

        if (useSeparator) {
            if (LocaleController.isRTL) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, linePaint);
            } else {
                canvas.drawLine(AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, linePaint);
            }
        }

        if (drawAlpha != 1) {
            canvas.saveLayerAlpha(0, 0, canvas.getWidth(), canvas.getHeight(), (int)(255 * drawAlpha), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        }

        /*if (drawNameLock) {
            setDrawableBounds(lockDrawable, nameLockLeft, nameLockTop);
            lockDrawable.draw(canvas);
        } else*/ if (drawNameGroup) {
            setDrawableBounds(groupDrawable, nameLockLeft, nameLockTop);
            groupDrawable.draw(canvas);
        }

        if (nameLayout != null) {
            canvas.save();
            canvas.translate(nameLeft, nameTop);
            nameLayout.draw(canvas);
            canvas.restore();
        }

        if (onlineLayout != null) {
            canvas.save();
            canvas.translate(onlineLeft, AndroidUtilities.dp(40));
            onlineLayout.draw(canvas);
            canvas.restore();
        }

        if (countLayout != null) {
            if (MessagesController.getInstance().isDialogMuted(dialog_id)) {
                setDrawableBounds(countDrawableGrey, countLeft - AndroidUtilities.dp(5.5f), countTop, countWidth + AndroidUtilities.dp(11), countDrawableGrey.getIntrinsicHeight());
                countDrawableGrey.draw(canvas);
            } else {
                setDrawableBounds(countDrawable, countLeft - AndroidUtilities.dp(5.5f), countTop, countWidth + AndroidUtilities.dp(11), countDrawable.getIntrinsicHeight());
                countDrawable.draw(canvas);
            }
            canvas.save();
            canvas.translate(countLeft, countTop + AndroidUtilities.dp(4));
            countLayout.draw(canvas);
            canvas.restore();
        }

        avatarImage.draw(canvas);
    }
}
