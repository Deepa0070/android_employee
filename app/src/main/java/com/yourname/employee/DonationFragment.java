package com.yourname.employee;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import models.DonationRequest;

public class DonationFragment extends Fragment {

    private static final String ARG_TYPE = "type";
    private static final String ARG_ROLE = "role";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<DonationRequest> donationList;
    private DonationAdapter adapter;

    private String fragmentType;
    private String userRole;

    public DonationFragment() {
        // Required empty public constructor
    }

    public static DonationFragment newInstance(String type, String userRole) {
        DonationFragment fragment = new DonationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_ROLE, userRole);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fragmentType = getArguments().getString(ARG_TYPE);
            userRole = getArguments().getString(ARG_ROLE);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        donationList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donation, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DonationAdapter(donationList, getContext(), userRole);
        recyclerView.setAdapter(adapter);

        loadDonations();

        return view;
    }

    private void loadDonations() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        switch (fragmentType) {
            case "donor":
                loadDonorDonations();
                break;
            case "recipient":
                loadRecipientDonations();
                break;
            case "all":
                loadAllDonations();
                break;
        }
    }

    private void loadDonorDonations() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("donationRequests")
                .whereEqualTo("donorId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DonationRequest donation = document.toObject(DonationRequest.class);
                        donation.setId(document.getId());
                        donationList.add(donation);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setText("Error loading donations");
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void loadRecipientDonations() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("donationRequests")
                .whereEqualTo("recipientId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DonationRequest donation = document.toObject(DonationRequest.class);
                        donation.setId(document.getId());
                        donationList.add(donation);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setText("Error loading donations");
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void loadAllDonations() {
        // Load ALL donations - this is for HR
        db.collection("donationRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DonationRequest donation = document.toObject(DonationRequest.class);
                        donation.setId(document.getId());
                        donationList.add(donation);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setText("Error loading donations");
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();

        if (donationList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No donations found");
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDonations();
    }
}