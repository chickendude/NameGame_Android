package com.willowtreeapps.namegame.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.ListRandomizer;
import com.willowtreeapps.namegame.core.NameGameApplication;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;
import com.willowtreeapps.namegame.util.CircleBorderTransform;
import com.willowtreeapps.namegame.util.Ui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class NameGameFragment extends Fragment implements ProfilesRepository.Listener {
	private static final String TAG = NameGameFragment.class.getSimpleName();

	private static final Interpolator OVERSHOOT = new OvershootInterpolator();
	private static final String[] QUESTIONS = {
			"Can you tell me who %s is?",
			"Who is %s?",
			"And how about %s?",
			"And %s?"};

	@Inject
	ListRandomizer listRandomizer;
	@Inject
	Picasso picasso;
	@Inject
	ProfilesRepository profilesRepository;

	// Views
	private TextView title;
	private ViewGroup container;
	private List<ImageView> faces = new ArrayList<>(6);

	// Objects
	private List<Person> people;
	private List<Person> testSet;
	private Person testAnswer;
	private int numQuestions;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NameGameApplication.get(getActivity()).component().inject(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.name_game_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		title = view.findViewById(R.id.title);
		container = view.findViewById(R.id.face_container);

		numQuestions = 0;

		//Hide the views until data loads
		title.setAlpha(0);

		int n = container.getChildCount();
		for (int i = 0; i < n; i++) {
			ImageView face = (ImageView) container.getChildAt(i);
			faces.add(face);

			//Hide the views until data loads
			face.setScaleX(0);
			face.setScaleY(0);
		}

		// load values from API
		profilesRepository.register(this);
	}

	/**
	 * A method for setting the images from people into the imageviews
	 */
	private void setImages(List<ImageView> faces, List<Person> people) {
		int imageSize = (int) Ui.convertDpToPixel(100, getContext());
		int n = faces.size();

		for (int i = 0; i < n; i++) {
			ImageView face = faces.get(i);
			final Person person = people.get(i);
			face.setOnClickListener(imageView -> onPersonSelected(imageView, person));
			picasso.load("http:" + person.getHeadshot().getUrl())
					.placeholder(R.drawable.ic_face_white_48dp)
					.resize(imageSize, imageSize)
					.transform(new CircleBorderTransform(getContext()))
					.into(face);
		}
		animateFacesIn();
	}

	/**
	 * A method to animate the faces into view
	 */
	private void animateFacesIn() {
		title.animate().alpha(1).start();
		for (int i = 0; i < faces.size(); i++) {
			ImageView face = faces.get(i);
			face.animate().scaleX(1).scaleY(1).setStartDelay(800 + 120 * i).setInterpolator(OVERSHOOT).start();
		}
	}

	/**
	 * A method to handle when a person is selected
	 *
	 * @param view   The view that was selected
	 * @param person The person that was selected
	 */
	private void onPersonSelected(@NonNull View view, @NonNull Person person) {
		//TODO evaluate whether it was the right person and make an action based on that
		String msg = person.equals(testAnswer) ? "Correct!" : "Sorry, that was not correct!";
		Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
		getNextTestSet();
	}

	private void getNextTestSet() {
		numQuestions++;
		testSet = listRandomizer.pickN(people, 6);
		testAnswer = listRandomizer.pickOne(testSet);
		String name = testAnswer.getFirstName() + " " + testAnswer.getLastName();
		title.setText(String.format("Who is %s?", name));
		setImages(faces, testSet);
	}

	@Override
	public void onLoadFinished(@NonNull List<Person> people) {
		Log.d(TAG, people.toString());
		this.people = people;
		getNextTestSet();
	}

	@Override
	public void onError(@NonNull Throwable error) {
		Toast.makeText(getContext(), "There was an error getting the list of names!", Toast.LENGTH_LONG).show();
		Log.d(TAG, error.getLocalizedMessage());
	}


}
