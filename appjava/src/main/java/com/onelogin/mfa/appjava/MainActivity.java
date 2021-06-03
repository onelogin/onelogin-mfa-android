package com.onelogin.mfa.appjava;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.appjava.factors.FactorsActivity;
import com.onelogin.mfa.appjava.register.QrActivity;
import com.onelogin.mfa.appjava.register.WebLoginActivity;

import java.util.Objects;

@SuppressWarnings("UnusedReturnValue")
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String MANUAL_ENTRY_SUCCESS_TILE = "manualEntrySuccess";
    public static final int MANUAL_ENTRY_SUCCESS_CODE = 123;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpNavigation();

        Fragment homeFragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container_frame, homeFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed(){
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.nav_home);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        displaySelectedView(item.getItemId());
        return true;
    }

    private void setUpNavigation() {
        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);

        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @SuppressWarnings("SameReturnValue")
    private boolean displaySelectedView(int menuItemId) {
        Fragment fragment = null;
        Intent intent = null;

        if (menuItemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (menuItemId == R.id.nav_factors) {
            intent = new Intent(this, FactorsActivity.class);
        } else if (menuItemId == R.id.nav_oidc) {
            Toast.makeText(this, "Click OIDC", Toast.LENGTH_SHORT).show();
        } else if (menuItemId == R.id.nav_web_login) {
            intent = new Intent(this, WebLoginActivity.class);
        } else if (menuItemId == R.id.nav_qr_or_manual) {
            intent = new Intent(this, QrActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
        } else if (fragment != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.container_frame, fragment);
            fragmentTransaction.commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START); return true;
    }
}