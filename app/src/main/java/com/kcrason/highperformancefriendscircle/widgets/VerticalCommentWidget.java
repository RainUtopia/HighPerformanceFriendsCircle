package com.kcrason.highperformancefriendscircle.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kcrason.highperformancefriendscircle.R;
import com.kcrason.highperformancefriendscircle.enums.TranslationState;
import com.kcrason.highperformancefriendscircle.interfaces.OnItemClickPopupMenuListener;
import com.kcrason.highperformancefriendscircle.Utils;
import com.kcrason.highperformancefriendscircle.beans.CommentBean;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * @author KCrason
 * @date 2018/4/27
 */
public class VerticalCommentWidget extends LinearLayout implements ViewGroup.OnHierarchyChangeListener,
        OnItemClickPopupMenuListener {

    private List<CommentBean> mCommentBeans;

    private SimpleWeakObjectPool<View> COMMENT_TEXT_POOL;

    public VerticalCommentWidget(Context context) {
        super(context);
        init();
    }

    public VerticalCommentWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VerticalCommentWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        COMMENT_TEXT_POOL = new SimpleWeakObjectPool<>();
        setOnHierarchyChangeListener(this);
    }


    public void addComments(List<CommentBean> commentBeans, boolean isStartAnimation) {
        this.mCommentBeans = commentBeans;
        if (commentBeans != null) {
            int oldCount = getChildCount();
            int newCount = commentBeans.size();
            if (oldCount > newCount) {
                removeViewsInLayout(newCount, oldCount - newCount);
            }
            for (int i = 0; i < newCount; i++) {
                boolean hasChild = i < oldCount;
                View childView = hasChild ? getChildAt(i) : null;
                CommentBean commentBean = commentBeans.get(i);
                SpannableStringBuilder commentSpan = commentBean.getCommentContentSpan();
                TranslationState translationState = commentBean.getTranslationState();
                if (childView == null) {
                    childView = COMMENT_TEXT_POOL.get();
                    if (childView == null) {
                        addViewInLayout(makeCommentItemView(commentSpan, i, translationState, isStartAnimation), i, generateDefaultLayoutParams(), true);
                    } else {
                        addCommentItemView(childView, commentSpan, i, translationState, isStartAnimation);
                    }
                } else {
                    updateCommentData(childView, commentSpan, i, translationState, isStartAnimation);
                }
            }
            requestLayout();
        }
    }


    /**
     * 更新指定的position的comment
     */
    public void updateTargetComment(int position, List<CommentBean> commentBeans) {
        int oldCount = getChildCount();
        for (int i = 0; i < oldCount; i++) {
            if (i == position) {
                View childView = getChildAt(i);
                if (childView != null) {
                    CommentBean commentBean = commentBeans.get(i);
                    SpannableStringBuilder commentSpan = commentBean.getCommentContentSpan();
                    TranslationState translationState = commentBean.getTranslationState();
                    updateCommentData(childView, commentSpan, i, translationState, true);
                }
                break;
            }
        }
        requestLayout();
    }


    /**
     * 創建Comment item view
     */
    private View makeCommentItemView(SpannableStringBuilder content, int index, TranslationState translationState, boolean isStartAnimation) {
        if (translationState == TranslationState.START) {
            return makeContentTextView(content, index);
        } else {
            return new CommentTranslationLayoutView(getContext())
                    .setOnItemClickPopupMenuListener(this)
                    .setCurrentPosition(index)
                    .setSourceContent(content)
                    .setTranslationContent(content)
                    .setTranslationState(translationState, isStartAnimation);
        }
    }


    /**
     * 添加需要的Comment View
     */
    private void addCommentItemView(View view, SpannableStringBuilder builder, int index, TranslationState translationState, boolean isStartAnimation) {
        if (view instanceof CommentTranslationLayoutView) {
            if (translationState == TranslationState.START) {
                addViewInLayout(makeCommentItemView(builder, index, translationState, isStartAnimation), index, generateDefaultLayoutParams(), true);
            } else {
                CommentTranslationLayoutView translationLayoutView = (CommentTranslationLayoutView) view;
                translationLayoutView.setOnItemClickPopupMenuListener(this).setCurrentPosition(index).setSourceContent(builder).setTranslationContent(builder);
                addViewInLayout(translationLayoutView, index, generateDefaultLayoutParams(), true);
            }
        } else if (view instanceof TextView) {
            if (translationState == TranslationState.START) {
                ((TextView) view).setText(builder);
                addOnItemClickPopupMenuListener(view, index, TranslationState.START);
                addViewInLayout(view, index, generateDefaultLayoutParams(), true);
            } else {
                addViewInLayout(makeCommentItemView(builder, index, translationState, isStartAnimation), index, generateDefaultLayoutParams(), true);
            }
        }
    }


    private void addOnItemClickPopupMenuListener(View view, int index, TranslationState translationState) {
        view.setOnLongClickListener(v -> {
            Utils.showPopupMenu(getContext(), VerticalCommentWidget.this, index, v, translationState);
            return false;
        });
    }

    /**
     * 更新comment list content
     */
    private void updateCommentData(View view, SpannableStringBuilder builder, int index, TranslationState translationState, boolean isStartAnimation) {
        if (view instanceof CommentTranslationLayoutView) {
            if (translationState == TranslationState.START) {
                addViewInLayout(makeCommentItemView(builder, index, translationState, isStartAnimation), index, generateDefaultLayoutParams(), true);
                removeViewInLayout(view);
            } else {
                CommentTranslationLayoutView translationLayoutView = (CommentTranslationLayoutView) view;
                translationLayoutView.setCurrentPosition(index)
                        .setSourceContent(builder)
                        .setTranslationContent(builder)
                        .setTranslationState(translationState, isStartAnimation);
            }
        } else if (view instanceof TextView) {
            if (translationState == TranslationState.START) {
                ((TextView) view).setText(builder);
            } else {
                addViewInLayout(makeCommentItemView(builder, index, translationState, isStartAnimation), index, generateDefaultLayoutParams(), true);
                removeViewInLayout(view);
            }
        }
    }

    private TextView makeContentTextView(SpannableStringBuilder content, int index) {
        TextView textView = new TextView(getContext());
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.base_333333));
        textView.setBackgroundResource(R.drawable.selector_view_name_state);
        textView.setTextSize(16f);
        textView.setText(content);
        textView.setLineSpacing(Utils.dp2px(getContext(), 3f), 1f);
        addOnItemClickPopupMenuListener(textView, index, TranslationState.START);
        return textView;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {

    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        COMMENT_TEXT_POOL.put(child);
    }

    @Override
    public void onItemClickCopy(int position) {
        Toast.makeText(getContext(), "You Click" + position + " Copy!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClickTranslation(int position) {
        if (mCommentBeans != null && position < mCommentBeans.size()) {
            mCommentBeans.get(position).setTranslationState(TranslationState.CENTER);
            updateTargetComment(position, mCommentBeans);
            timerTranslation(position);
        }
    }

    @SuppressLint("CheckResult")
    private void timerTranslation(final int position) {
        Single.timer(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(aLong -> {
            if (mCommentBeans != null && position < mCommentBeans.size()) {
                mCommentBeans.get(position).setTranslationState(TranslationState.END);
                updateTargetComment(position, mCommentBeans);
            }
        });
    }

    @Override
    public void onItemClickHideTranslation(int position) {
        if (mCommentBeans != null && position < mCommentBeans.size()) {
            mCommentBeans.get(position).setTranslationState(TranslationState.START);
            updateTargetComment(position, mCommentBeans);
        }
    }

    @Override
    public void onItemClickCollection(int position) {
        Toast.makeText(getContext(), "You Click" + position + "  Collection!", Toast.LENGTH_SHORT).show();
    }


    final class SimpleWeakObjectPool<T> {

        private WeakReference<T>[] objsPool;
        private int size;
        private int curPointer = -1;


        public SimpleWeakObjectPool() {
            this(5);
        }

        public SimpleWeakObjectPool(int size) {
            this.size = size;
            objsPool = (WeakReference<T>[]) Array.newInstance(WeakReference.class, size);
        }

        public synchronized T get() {
            if (curPointer == -1 || curPointer > objsPool.length) return null;
            T obj = objsPool[curPointer].get();
            objsPool[curPointer] = null;
            curPointer--;
            return obj;
        }

        public synchronized boolean put(T t) {
            if (curPointer == -1 || curPointer < objsPool.length - 1) {
                curPointer++;
                objsPool[curPointer] = new WeakReference<T>(t);
                return true;
            }
            return false;
        }

        public void clearPool() {
            for (int i = 0; i < objsPool.length; i++) {
                objsPool[i].clear();
                objsPool[i] = null;
            }
            curPointer = -1;
        }

        public int size() {
            return objsPool == null ? 0 : objsPool.length;
        }
    }


}
