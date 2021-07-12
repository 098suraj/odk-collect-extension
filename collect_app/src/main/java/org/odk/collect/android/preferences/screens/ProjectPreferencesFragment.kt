/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.odk.collect.android.preferences.screens

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import org.odk.collect.android.R
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.preferences.dialogs.AdminPasswordDialogFragment
import org.odk.collect.android.preferences.dialogs.AdminPasswordViewModel
import org.odk.collect.android.preferences.dialogs.ChangeAdminPasswordDialog
import org.odk.collect.android.preferences.dialogs.ChangeAdminPasswordViewModel
import org.odk.collect.android.preferences.keys.AdminKeys
import org.odk.collect.android.utilities.AdminPasswordProvider
import org.odk.collect.android.utilities.DialogUtils
import org.odk.collect.android.utilities.MultiClickGuard
import org.odk.collect.android.utilities.ToastUtils
import org.odk.collect.android.version.VersionInformation
import javax.inject.Inject

class ProjectPreferencesFragment :
    BaseProjectPreferencesFragment(),
    Preference.OnPreferenceClickListener {

    @Inject
    lateinit var versionInformation: VersionInformation

    @Inject
    lateinit var adminPasswordProvider: AdminPasswordProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerUtils.getComponent(context).inject(this)
        setHasOptionsMenu(true)

        val changeAdminPasswordViewModel = ViewModelProvider(requireActivity()).get(
            ChangeAdminPasswordViewModel::class.java
        )
        changeAdminPasswordViewModel.passwordEnabled.observe(
            this,
            { isPasswordEnabled: Boolean ->
                if (isPasswordEnabled) {
                    isPasswordEntered = true
                }
                isPasswordSet = isPasswordEnabled
                requireActivity().invalidateOptionsMenu()
            }
        )

        val adminPasswordViewModel = ViewModelProvider(requireActivity()).get(
            AdminPasswordViewModel::class.java
        )
        adminPasswordViewModel.passwordEntered.observe(
            this,
            { isPasswordCorrect: Boolean ->
                if (isPasswordCorrect) {
                    isPasswordEntered = true
                    requireActivity().invalidateOptionsMenu()
                } else {
                    ToastUtils.showShortToast(R.string.admin_password_incorrect)
                }
            }
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.project_preferences, rootKey)

        findPreference<Preference>(PROTOCOL_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(PROJECT_DISPLAY_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(USER_INTERFACE_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(MAPS_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(FORM_MANAGEMENT_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(EXPERIMENTAL_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(AdminKeys.KEY_CHANGE_ADMIN_PASSWORD)!!.onPreferenceClickListener = this
        findPreference<Preference>(PROJECT_MANAGEMENT_PREFERENCE_KEY)!!.onPreferenceClickListener = this
        findPreference<Preference>(ACCESS_CONTROL_PREFERENCE_KEY)!!.onPreferenceClickListener = this

        if (!isInAdminMode) {
            setPreferencesVisibility()
        }
        if (versionInformation.isRelease) {
            findPreference<Preference>(EXPERIMENTAL_PREFERENCE_KEY)!!.isVisible = false
        }
        if (adminPasswordProvider.isAdminPasswordSet) {
            isPasswordSet = true
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (MultiClickGuard.allowClick(javaClass.name)) {
            when (preference.key) {
                PROTOCOL_PREFERENCE_KEY -> displayPreferences(ServerPreferencesFragment())
                PROJECT_DISPLAY_PREFERENCE_KEY -> displayPreferences(ProjectDisplayPreferencesFragment())
                USER_INTERFACE_PREFERENCE_KEY -> displayPreferences(UserInterfacePreferencesFragment())
                MAPS_PREFERENCE_KEY -> displayPreferences(MapsPreferencesFragment())
                FORM_MANAGEMENT_PREFERENCE_KEY -> displayPreferences(FormManagementPreferencesFragment())
                USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY -> displayPreferences(IdentityPreferencesFragment())
                EXPERIMENTAL_PREFERENCE_KEY -> displayPreferences(ExperimentalPreferencesFragment())
                AdminKeys.KEY_CHANGE_ADMIN_PASSWORD -> {
                    if (isPasswordSet && !isPasswordEntered) {
                        DialogUtils.showIfNotShowing(AdminPasswordDialogFragment::class.java, requireActivity().supportFragmentManager)
                    } else {
                        DialogUtils.showIfNotShowing(
                            ChangeAdminPasswordDialog::class.java, requireActivity().supportFragmentManager
                        )
                    }
                }
                PROJECT_MANAGEMENT_PREFERENCE_KEY -> {
                    if (isPasswordSet && !isPasswordEntered) {
                        DialogUtils.showIfNotShowing(AdminPasswordDialogFragment::class.java, requireActivity().supportFragmentManager)
                    } else {
                        displayPreferences(ProjectManagementPreferencesFragment())
                    }
                }
                ACCESS_CONTROL_PREFERENCE_KEY -> {
                    if (isPasswordSet && !isPasswordEntered) {
                        DialogUtils.showIfNotShowing(AdminPasswordDialogFragment::class.java, requireActivity().supportFragmentManager)
                    } else {
                        displayPreferences(AccessControlPreferencesFragment())
                    }
                }
            }
            return true
        }
        return false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (isPasswordSet) {
            if (isPasswordEntered) {
                menu.findItem(R.id.menu_unlocked).isVisible = true
            } else {
                menu.findItem(R.id.menu_locked).isVisible = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.project_preferences_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_locked) {
            DialogUtils.showIfNotShowing(AdminPasswordDialogFragment::class.java, requireActivity().supportFragmentManager)
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        isPasswordEntered = false
    }

    private fun displayPreferences(fragment: Fragment?) {
        if (fragment != null) {
            fragment.arguments = arguments
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.preferences_fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setPreferencesVisibility() {
        val preferenceScreen = preferenceScreen
        if (!hasAtleastOneSettingEnabled(AdminKeys.serverKeys)) {
            preferenceScreen.removePreference(findPreference(PROTOCOL_PREFERENCE_KEY))
        }
        if (!hasAtleastOneSettingEnabled(AdminKeys.userInterfaceKeys)) {
            preferenceScreen.removePreference(findPreference(USER_INTERFACE_PREFERENCE_KEY))
        }
        val mapsScreenEnabled = settingsProvider.getAdminSettings().getBoolean(AdminKeys.KEY_MAPS)
        if (!mapsScreenEnabled) {
            preferenceScreen.removePreference(findPreference(MAPS_PREFERENCE_KEY))
        }
        if (!hasAtleastOneSettingEnabled(AdminKeys.formManagementKeys)) {
            preferenceScreen.removePreference(findPreference(FORM_MANAGEMENT_PREFERENCE_KEY))
        }
        if (!hasAtleastOneSettingEnabled(AdminKeys.identityKeys)) {
            preferenceScreen.removePreference(findPreference(USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY))
        }
    }

    private fun hasAtleastOneSettingEnabled(keys: Collection<String>): Boolean {
        for (key in keys) {
            val value = settingsProvider.getAdminSettings().getBoolean(key)
            if (value) {
                return true
            }
        }
        return false
    }

    fun preventOtherWaysOfEditingForm() {
        val fragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.preferences_fragment_container) as FormEntryAccessPreferencesFragment
        fragment.preventOtherWaysOfEditingForm()
    }

    companion object {
        private const val PROTOCOL_PREFERENCE_KEY = "protocol"
        private const val PROJECT_DISPLAY_PREFERENCE_KEY = "project_display"
        private const val USER_INTERFACE_PREFERENCE_KEY = "user_interface"
        private const val MAPS_PREFERENCE_KEY = "maps"
        private const val FORM_MANAGEMENT_PREFERENCE_KEY = "form_management"
        private const val USER_AND_DEVICE_IDENTITY_PREFERENCE_KEY = "user_and_device_identity"
        private const val EXPERIMENTAL_PREFERENCE_KEY = "experimental"
        private const val PROJECT_MANAGEMENT_PREFERENCE_KEY = "project_management"
        private const val ACCESS_CONTROL_PREFERENCE_KEY = "access_control"

        var isPasswordSet = false
        var isPasswordEntered = false
    }
}
