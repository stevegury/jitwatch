/*
 * Copyright (c) 2013, 2014 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.sandbox;

import org.adoptopenjdk.jitwatch.model.IMetaMember;

public interface ISandboxStage
{
	void openTriView(IMetaMember member);
	
	void showError(String error);
}