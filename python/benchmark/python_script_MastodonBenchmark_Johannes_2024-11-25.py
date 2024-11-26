import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
import os

# File paths (single or multiple files)
csv_files = [
    'C:/Users/johan/Documents/Work_Paternity-Leave/Tracking/Benchmark/Benchmark_Cube/Cube_final_benchmark_data_final/BDV_Settings_1/benchmark_measurements_Test1_2024-11-25_delete.csv',
    'C:/Users/johan/Documents/Work_Paternity-Leave/Tracking/Benchmark/Benchmark_Cube/Cube_final_benchmark_data_final/BDV_Settings_1/benchmark_measurements_Test2_2024-11-25_delete_normal.csv'
]

# Sources to plot
# sources = ["Avg per BigDataViewers", "Avg per TrackSchemes", "Avg per command"]  # Add more sources as needed
# sources = ["Avg per BigDataViewers", "Avg per TrackSchemes"]  # Add more sources as needed
sources = ["Avg per BigDataViewers"]

# Commands to consider only
commands = ["BDV_T"]


# Colors for multiple datasets and sources
colors = ['#0072B2', '#D55E00', '#009E73', '#CC79A7', '#D73027', '#F0E442', '#56B4E9']

# Output directory
output_dir = os.path.join(os.path.dirname(csv_files[0]), "benchmark_results_plots")
os.makedirs(output_dir, exist_ok=True)

# Function to strip `.dot.numbers` from column labels
def clean_column_labels(labels):
    return [re.sub(r'\.\d+', '', label) for label in labels]

# List rows matching the source
def list_rows_with_source(table, src: str) -> list[int]:
    return [i for i, row_header in enumerate(table['source']) if row_header.startswith(src)]

# Plot function with error bars
def plot_with_error_bars(file_paths, sources, output_dir):
    plt.figure(figsize=(8, 8))

    file_legend_labels = []  # For the external legend
    color_idx = 0  # Color index to assign different colors to each source-dataset combination

    # Iterate over all input CSV files
    for file_idx, file_path in enumerate(file_paths):
        if not os.path.exists(file_path):
            print(f"File not found: {file_path}")
            continue

        # Load CSV
        csv_table = pd.read_csv(file_path, delimiter='\t')
        csv_table.columns = csv_table.columns.str.strip()

        # Filter BDV_T columns dynamically
        # What about TS??
        bdv_columns = [col for col in csv_table.columns if re.match(r"BDV_T\d+", col)]
        csv_table[bdv_columns] = csv_table[bdv_columns].apply(pd.to_numeric, errors='coerce')

        # selected columns to only work with
        columns = bdv_columns
        columns_clean = clean_column_labels(columns)

        x_tics = range(len(columns_clean))

        # Iterate through the sources to plot them
        for source in sources:
            rows = list_rows_with_source(csv_table, source)
            if not rows:
                print(f"No rows found for source '{source}' in file {file_path}")
                continue

            # Collect data for error bars
            vals = np.zeros((len(columns), len(rows)), dtype=float)
            for ri, row_idx in enumerate(rows):
                for ci, col_label in enumerate(columns):
                    vals[ci, ri] = csv_table.at[row_idx, col_label]

            # Calculate mean and std for error bars
            means = np.nanmean(vals, axis=1)
            stds = np.nanstd(vals, axis=1)

            # Plot mean with error bars
            plt.errorbar(
                x_tics, means, yerr=stds, fmt='-o', capsize=5,
                label=f"Dataset {file_idx + 1}: {source} Mean ± Std",
                color=colors[color_idx % len(colors)]
            )
            color_idx += 1  # Increment color index

            # Add file information to external legend
            file_legend_labels.append(f"Dataset {file_idx + 1}: {source} from {os.path.basename(file_path)}")

    # Customize the plot
    plt.title("Comparison of Multiple Sources", fontsize=14)
    plt.ylabel("Command time (seconds)", fontsize=12)
    plt.xlabel("Individual commands", fontsize=12)
    plt.xticks(x_tics, columns_clean, rotation=45)

    # Add gridlines for better readability
    plt.grid(axis='y', linestyle='--', linewidth=0.5, alpha=0.7)

    # Add internal legend (for datasets and sources)
    plt.legend(loc='upper left', title="Plotted Information")

    # Add external legend (for file information)
    plt.gcf().text(1.02, 0.95, "File-to-Dataset Mapping:", fontsize=10, fontweight='bold', ha='left')
    for idx, label in enumerate(file_legend_labels):
        plt.gcf().text(1.02, 0.9 - idx * 0.05, label, fontsize=10, ha='left')

    # Save the plot
    output_file = os.path.join(output_dir, "comparison_plot_multiple_sources.png")
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches="tight")
    print(f"Plot saved to: {output_file}")

    # Show the plot
    plt.show()

# Call the function to plot with error bars
plot_with_error_bars(csv_files, sources, output_dir)
