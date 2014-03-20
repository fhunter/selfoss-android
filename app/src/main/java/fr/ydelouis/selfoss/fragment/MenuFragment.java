package fr.ydelouis.selfoss.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.IgnoredWhenDetached;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.Collections;
import java.util.List;

import fr.ydelouis.selfoss.R;
import fr.ydelouis.selfoss.account.SelfossAccount;
import fr.ydelouis.selfoss.account.SelfossAccountActivity_;
import fr.ydelouis.selfoss.entity.ArticleType;
import fr.ydelouis.selfoss.entity.Tag;
import fr.ydelouis.selfoss.model.DatabaseHelper;
import fr.ydelouis.selfoss.sync.TagSync;
import fr.ydelouis.selfoss.view.TagView;
import fr.ydelouis.selfoss.view.TagView_;

@EFragment(R.layout.fragment_menu)
public class MenuFragment extends Fragment implements View.OnClickListener {

	@Bean protected SelfossAccount account;
	@FragmentArg @InstanceState
	protected ArticleType type = ArticleType.Newest;
	@FragmentArg @InstanceState
	protected Tag tag = Tag.ALL;
	@OrmLiteDao(helper = DatabaseHelper.class, model = Tag.class)
	protected RuntimeExceptionDao<Tag, String> tagDao;
	private Listener listener = new DummyListener();

	@ViewById protected TextView url;
	@ViewById protected View newest;
	@ViewById protected View unread;
	@ViewById protected View favorite;
	@ViewById protected ViewGroup tagContainer;

	@Override
	public void onResume() {
		super.onResume();
		updateViews();
	}

	@AfterViews
	protected void updateViews() {
		url.setText(account.getUrl());
		selectType();
		loadAndUpdateTags();
	}

	private void selectType() {
		newest.setSelected(type == ArticleType.Newest);
		unread.setSelected(type == ArticleType.Unread);
		favorite.setSelected(type == ArticleType.Favorite);
	}

	@Background
	protected void loadAndUpdateTags() {
		List<Tag> tags = tagDao.queryForAll();
		sortAndUpdateTags(tags);
	}

	@Receiver(actions = {TagSync.ACTION_SYNC_TAGS}, registerAt = Receiver.RegisterAt.OnResumeOnPause)
	protected void onTagsSynced(Intent intent) {
		sortAndUpdateTags(intent.<Tag>getParcelableArrayListExtra(TagSync.EXTRA_TAGS));
	}

	protected void sortAndUpdateTags(List<Tag> tags) {
		Collections.sort(tags, Tag.COMPARATOR_UNREAD_INVERSE);
		int unreadAll = 0;
		for (Tag tag : tags) {
			unreadAll += tag.getUnread();
		}
		Tag.ALL.setUnread(unreadAll);
		tags.add(0, Tag.ALL);
		updateTags(tags);
	}

	@UiThread
	@IgnoredWhenDetached
	protected void updateTags(List<Tag> tags) {
		tagContainer.removeAllViews();
		for (Tag tag : tags) {
			TagView tagView = TagView_.build(getActivity());
			tagView.setTag(tag);
			tagView.setSelected(tag.equals(this.tag));
			tagView.setOnClickListener(this);
			tagContainer.addView(tagView);
			tagContainer.addView(newDivider());
		}
	}

	private View newDivider() {
		View view = new View(getActivity());
		view.setBackgroundResource(R.color.menu_divider);
		view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.divider_height)));
		return view;
	}

	private void selectTag() {
		for (int i = 0; i < tagContainer.getChildCount(); i++) {
			View view = tagContainer.getChildAt(i);
			if (view instanceof TagView) {
				TagView tagView = (TagView) view;
				tagView.setSelected(tagView.getTag().equals(this.tag));
			}
		}
	}

	@Click(R.id.url)
	protected void openSelfossAccountActivity() {
		SelfossAccountActivity_.intent(getActivity()).start();
		listener.onAccountActivityStarted();
	}

	@Click({ R.id.newest, R.id.unread, R.id.favorite})
	protected void onArticleTypeClick(View view) {
		ArticleType newType = ArticleType.fromId(view.getId());
		if (newType != type) {
			this.type = newType;
			selectType();
			listener.onArticleTypeChanged(type);
		}
	}

	@Override
	public void onClick(View view) {
		if (view instanceof TagView) {
			Tag newTag = ((TagView) view).getTag();
			if (!newTag.equals(tag)) {
				this.tag = newTag;
				selectTag();
				listener.onTagChanged(tag);
			}
		}
	}

	public void setListener(Listener listener) {
		this.listener = listener != null ? listener : new DummyListener();
	}

	public interface Listener {
		void onAccountActivityStarted();
		void onArticleTypeChanged(ArticleType type);
		void onTagChanged(Tag tag);
	}

	private class DummyListener implements Listener {

		@Override
		public void onAccountActivityStarted() {}

		@Override
		public void onArticleTypeChanged(ArticleType type) {}

		@Override
		public void onTagChanged(Tag tag) {}
	}
}
