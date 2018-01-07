package com.willowtreeapps.namegame.core;

import com.willowtreeapps.namegame.network.NetworkModule;
import com.willowtreeapps.namegame.ui.NameGameActivity;
import com.willowtreeapps.namegame.ui.NameGameFragment;
import com.willowtreeapps.namegame.ui.NameGameLandingFragment;
import com.willowtreeapps.namegame.ui.NameGameScoreFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        NetworkModule.class
})
public interface ApplicationComponent {
    void inject(NameGameActivity activity);
    void inject(NameGameFragment fragment);
	void inject(NameGameLandingFragment nameGameLandingFragment);
	void inject(NameGameScoreFragment nameGameScoreFragment);
}