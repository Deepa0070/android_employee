package com.yourname.employee;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<Employee> employeeList;
    private Context context;

    public EmployeeAdapter(List<Employee> employeeList, Context context) {
        this.employeeList = employeeList;
        this.context = context;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        if (employeeList == null || position >= employeeList.size()) {
            return;
        }

        Employee employee = employeeList.get(position);

        // Set employee data to views with null checks
        holder.tvName.setText(employee.getName() != null ? employee.getName() : "Unknown");
        holder.tvEmail.setText(employee.getEmail() != null ? employee.getEmail() : "No Email");
        holder.tvDepartment.setText("Dept: " + (employee.getDepartment() != null ? employee.getDepartment() : "Not Assigned"));
        holder.tvDesignation.setText("Designation: " + (employee.getDesignation() != null ? employee.getDesignation() : "Not Assigned"));
        holder.tvRole.setText("Role: " + (employee.getRole() != null ? employee.getRole().toUpperCase() : "EMPLOYEE"));

        // Show leave balances
        String leaveBalance = String.format("Leaves: Casual %d | Sick %d | LOP %d",
                employee.getCasualLeave(),
                employee.getSickLeave(),
                employee.getLopBalance());
        holder.tvLeaveBalance.setText(leaveBalance);

        // Different background color for HR vs Employees
        if (employee.getRole() != null && (employee.getRole().equalsIgnoreCase("hr admin") ||
                employee.getRole().equalsIgnoreCase("hr") ||
                employee.getRole().equalsIgnoreCase("admin"))) {
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.colorHRCard));
            holder.tvRole.setTextColor(context.getResources().getColor(android.R.color.holo_purple));
        } else {
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.colorEmployeeCard));
            holder.tvRole.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        }
    }

    @Override
    public int getItemCount() {
        return employeeList != null ? employeeList.size() : 0;
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvDepartment, tvDesignation, tvRole, tvLeaveBalance;
        CardView cardView;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvLeaveBalance = itemView.findViewById(R.id.tvLeaveBalance);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}