import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
import os

# Toggle plots for different datasets
plot_bdv_t = False  # Set to True to plot BDV_T (BigDataViewer), False to skip
plot_bdv_r = False  # Set to True to plot BDV_Rn (BigDataViewer Rot), False to skip
plot_ts_b = True   # Set to True to plot TS_B (TrackScheme), False to skip
split_ts_b = True  # Set to True to split TS_B by bookmarks (e.g., TS_B1, TS_B2), False to group them by BDV_T

# File paths for the CSV files
file_paths = [
    'C:/Users/johan/Documents/Work_Paternity-Leave/Tracking/Benchmark/Benchmark_Cube/Cube_final_benchmark_data_final/BDV_Settings_1/benchmark_measurements_Test_2024-11-21_SingleArrayMemPool_10x_Tp1-22_with-edges_v2.csv',
    'C:/Users/johan/Documents/Work_Paternity-Leave/Tracking/Benchmark/Benchmark_Cube/Cube_final_benchmark_data_final/BDV_Settings_1/benchmark_measurements_Test_2024-11-20_SingleArrayMemPool_10x_Tp1-22_no-edges_v2.csv',
    'C:/Users/johan/Documents/Work_Paternity-Leave/Tracking/Benchmark/Benchmark_Cube/Cube_final_benchmark_data_final/BDV_Settings_1/benchmark_measurements_Test_2024-11-21_SingleArrayMemPool_10x_Tp1-22_with-edges_Workstation.csv'
]

# Optional custom names for the datasets
custom_names = [
    "Cubes with Lineage - Laptop",
    "Cubes without Lineage - Laptop",
    "Cubes with Lineage - Workstation",
]

# Improved colorblind-friendly palette
colors = [
    '#0072B2',  # Blue
    '#D55E00',  # Orange
    '#009E73',  # Green
    '#CC79A7',  # Purple
    '#D73027',  # Red
    '#56B4E9',  # Teal
    '#999999',  # Gray
    '#117733',  # Dark Green
    '#882255',  # Burgundy
    '#44AA99',  # Light Teal
    '#332288',  # Dark Blue
    '#88CCEE'   # Light Cyan
]

# Create output directory (assuming all CSV files are in the same folder)
output_dir = os.path.join(os.path.dirname(file_paths[0]), "benchmark_results_plots")
os.makedirs(output_dir, exist_ok=True)

# Initialize a plot
plt.figure(figsize=(7, 7))

# Loop through each file and process the data
for idx, file_path in enumerate(file_paths):
    # Load the dataset
    data = pd.read_csv(file_path, delimiter='\t')

    # Count occurrences of each type of column
    bdv_t_headers = [col for col in data.columns if col.startswith("BDV_T")]
    bdv_r_headers = [col for col in data.columns if col.startswith("BDV_R")]
    ts_b_headers = [col for col in data.columns if col.startswith("TS_B")]

    # Extract BenchBDV rows and BDV_Ty columns
    bench_bdv_rows = data[data['source'].str.contains("BenchBDV #", na=False)]
    bdv_columns_bench = [col for col in data.columns if col.startswith("BDV_T") and '.' not in col]
    bdv_timepoints = [int(re.search(r'T(\d+)', col).group(1)) for col in bdv_columns_bench]

    # Extract Avg per command row
    avg_per_command_row = data[data['source'] == 'Avg per command']

    # Calculate the mean and standard deviation across BenchBDV runs
    bench_bdv_values = bench_bdv_rows[bdv_columns_bench].values
    mean_bdv = bench_bdv_values.mean(axis=0)  # Mean across runs
    std_bdv = bench_bdv_values.std(axis=0)   # Standard deviation across runs

    # Plot BDV_T if enabled
    if plot_bdv_t:
        bdv_t_label = f"{custom_names[idx] if idx < len(custom_names) else f'Dataset {idx + 1}'} (BDV)"
        plt.errorbar(
            bdv_timepoints,
            avg_per_command_values,
            yerr=std_bdv,
            fmt=f'-{markers[idx % len(markers)]}',
            capsize=5,
            label=bdv_t_label,
            color=colors[idx % len(colors)]
        )

    # Extract BDV_Rn columns and map them to the closest preceding BDV_Ty timepoints
    bdv_r_mapping = {}
    current_bdv_timepoint = None

    for col in data.columns:
        if col.startswith("BDV_T"):
            current_bdv_timepoint = int(re.search(r'T(\d+)', col).group(1))
        elif col.startswith("BDV_R") and current_bdv_timepoint is not None:
            bdv_r_mapping[col] = current_bdv_timepoint

    # Group BDV_Rn values by their associated BDV_Ty timepoints
    bdv_r_grouped = {}
    for bdv_r_col, bdv_timepoint in bdv_r_mapping.items():
        if bdv_timepoint not in bdv_r_grouped:
            bdv_r_grouped[bdv_timepoint] = []
        bdv_r_grouped[bdv_timepoint].append(bdv_r_col)

    # Plot BDV_R if enabled
    if plot_bdv_r:
        bdv_r_avg = []
        bdv_r_std = []
        bdv_r_timepoints = sorted(bdv_r_grouped.keys())

        for timepoint in bdv_r_timepoints:
            bdv_r_values = data[bdv_r_grouped[timepoint]].mean(axis=1)  # Mean across BDV_Rn columns for this timepoint
            bdv_r_avg.append(bdv_r_values.mean())  # Average across rows
            bdv_r_std.append(bdv_r_values.std())  # Standard deviation across rows

        bdv_r_label = f"{custom_names[idx] if idx < len(custom_names) else f'Dataset {idx + 1}'} (BDV Rotating)"
        plt.errorbar(
            bdv_r_timepoints,
            bdv_r_avg,
            yerr=bdv_r_std,
            fmt=f'--{markers[(idx + 1) % len(markers)]}',
            capsize=5,
            label=bdv_r_label,
            color=colors[(idx + len(file_paths)) % len(colors)]
        )

    # Group TS_B by either bookmarks or BDV_T
    if plot_ts_b:
        if split_ts_b:
            # Split TS_B by individual bookmarks
            ts_b_individual_groups = {}
            for col in ts_b_headers:
                bookmark_id = re.search(r'TS_B(\d+)', col).group(1)
                if bookmark_id not in ts_b_individual_groups:
                    ts_b_individual_groups[bookmark_id] = []
                ts_b_individual_groups[bookmark_id].append(col)

            # Plot each TS_B group
            for idx_ts_b, (bookmark_id, ts_b_cols) in enumerate(ts_b_individual_groups.items()):
                ts_b_values = data[ts_b_cols].mean(axis=1)  # Mean across TS_B columns for this bookmark
                ts_b_timepoints = bdv_timepoints  # Assume TS_B columns align with BDV_T timepoints
                ts_b_avg = ts_b_values.mean()
                ts_b_std = ts_b_values.std()

                ts_b_label = f"{custom_names[idx] if idx < len(custom_names) else f'Dataset {idx + 1}'} (TS BM {bookmark_id})"
                ts_b_color = colors[(idx_ts_b + 3 * len(file_paths)) % len(colors)]  # Unique color for each TS_B group

                plt.errorbar(
                    ts_b_timepoints,
                    [ts_b_avg] * len(ts_b_timepoints),  # Uniform value across timepoints
                    yerr=[ts_b_std] * len(ts_b_timepoints),  # Uniform error bars
                    fmt=f':{markers[(idx + 2) % len(markers)]}',
                    capsize=5,
                    label=ts_b_label,
                    color=ts_b_color
                )
        else:
            # Group TS_B by BDV_T
            ts_b_mapping = {}
            current_bdv_timepoint = None

            for col in data.columns:
                if col.startswith("BDV_T"):
                    current_bdv_timepoint = int(re.search(r'T(\d+)', col).group(1))
                elif col.startswith("TS_B") and current_bdv_timepoint is not None:
                    ts_b_mapping[col] = current_bdv_timepoint

            # Group TS_B values by their associated BDV_Ty timepoints
            ts_b_grouped = {}
            for ts_b_col, bdv_timepoint in ts_b_mapping.items():
                if bdv_timepoint not in ts_b_grouped:
                    ts_b_grouped[bdv_timepoint] = []
                ts_b_grouped[bdv_timepoint].append(ts_b_col)

            # Calculate and plot TS_B values grouped by BDV_T
            ts_b_avg = []
            ts_b_std = []
            ts_b_timepoints = sorted(ts_b_grouped.keys())

            for timepoint in ts_b_timepoints:
                ts_b_values = data[ts_b_grouped[timepoint]].mean(axis=1)  # Mean across TS_B columns for this timepoint
                ts_b_avg.append(ts_b_values.mean())  # Average across rows
                ts_b_std.append(ts_b_values.std())  # Standard deviation across rows

            ts_b_label = f"{custom_names[idx] if idx < len(custom_names) else f'Dataset {idx + 1}'} (TS)"
            ts_b_color = colors[(idx + 3 * len(file_paths)) % len(colors)]  # Unique color for grouped TS_B

            plt.errorbar(
                ts_b_timepoints,
                ts_b_avg,
                yerr=ts_b_std,
                fmt=f':{markers[(idx + 2) % len(markers)]}',
                capsize=5,
                label=ts_b_label,
                color=ts_b_color
            )

# Customize the plot
plt.title("Performance Comparison Across Datasets")
plt.xlabel("Frame/Timepoint")
plt.ylabel("Rendering Time (sec)")
plt.legend(title="Datasets")
plt.grid()
plt.tight_layout()

# Save the plot to the subfolder
output_file = os.path.join(output_dir, 'comparison_with_bdv_rn_ts_b_split_or_grouped_plot.png')
plt.savefig(output_file, dpi=300)
print(f"Plot saved to: {output_file}")

# Display the plot
plt.show()
