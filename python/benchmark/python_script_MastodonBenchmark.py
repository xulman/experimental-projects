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
sources = ["Avg per command"]

# Commands to consider only
commands = ["BDV", "TS"]
# commands = ["BDV_T", "TS_B"] # Add more commands as needed
# commands = ["BDV_T"]


# ==================================================================================================================

# Colors for multiple datasets and sources
colors = [
    '#0072B2',  # Blue
    '#D55E00',  # Vermilion
    '#CC79A7',  # Reddish Purple
    '#D73027',  # Red
    '#1B9E77',  # Teal
    '#7570B3',  # Violet
    '#66A61E',  # Olive
    '#E6AB02',  # Mustard Yellow
    '#A6CEE3',  # Light Blue
    '#FF7F00',  # Bright Orange
    '#6A3D9A',  # Purple
    '#FFFF99',  # Pale Yellow
    '#B15928'   # Dark Brown
    '#009E73',  # Green
    '#E69F00',  # Orange
    '#A6761D',  # Brown
    '#56B4E9',  # Sky Blue
]

# Output directory
output_dir = os.path.join(os.path.dirname(csv_files[0]), "benchmark_results_plots")
os.makedirs(output_dir, exist_ok=True)

# Function to strip `.dot.numbers` from column labels
def clean_column_labels(labels):
    return [re.sub(r'\.\d+', '', label) for label in labels]

# Function to put columns (that match the provided pattern) into a bold face font
def boldface_column_labels(labels, name_pattern):
    return [f"$\\bf{{{col.replace('_', '\\_')}}}$" if col.startswith(name_pattern) else col for col in labels]

# List rows matching the source
def list_rows_with_source(table, src: str) -> list[int]:
    return [i for i, row_header in enumerate(table['source']) if row_header.startswith(src)]

def list_rows_with_spots_counts(table) -> list[int]:
    return [i for i, row_header in enumerate(table['source']) if row_header.startswith('Spots in this time point')]

# Checks if the query_column is among any of the wanted_columns
def is_matching_column(query_column, wanted_columns):
    for w in wanted_columns:
        if re.match(f"{w}", query_column):
            return True
    return False


# Plot function with error bars
def plot_with_error_bars(file_paths, sources, output_dir):
    plt.figure(figsize=(16, 8))

    file_legend_labels = []  # For the external legend
    file_legend_colors = []  # To store the corresponding colors for the external legend
    color_idx = 0  # Color index to assign different colors to each source-dataset combination

    # Iterate over all input CSV files
    for file_idx, file_path in enumerate(file_paths):
        if not os.path.exists(file_path):
            print(f"File not found: {file_path}")
            continue

        # Load CSV
        csv_table = pd.read_csv(file_path, delimiter='\t')
        csv_table.columns = csv_table.columns.str.strip()

        # Selected columns to only work with
        columns = [col for col in csv_table.columns if is_matching_column(col, commands)]
        columns_clean = clean_column_labels(columns)
        csv_table[columns] = csv_table[columns].apply(pd.to_numeric, errors='coerce')

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

            # Select a color for this dataset-source combination
            color = colors[color_idx % len(colors)]

            # Plot mean with error bars
            plt.errorbar(
                x_tics, means, yerr=stds, fmt='-o', capsize=5,
                label=f"Dataset {file_idx + 1}: {source} Mean ± Std",
                color=color,
                markersize=5  # Change this value to adjust the size of the dots
            )
            color_idx += 1  # Increment color index

            # Add file information to external legend
            file_legend_labels.append(f"Dataset {file_idx + 1}: {source} from {os.path.basename(file_path)}")
            file_legend_colors.append(color)  # Store the color for the external legend

    # -----------------------------------------------------------------------
    # After all data is plotted, add axes and various visual tweaks
    # (IMPORTANT: assuming all data yielded the same 'columns')

    # Add vertical lines and boldfaced x-labels for BDV_T columns
    columns_highlight = boldface_column_labels(columns_clean, "BDV_T")
    bdv_t_indices = [i for i, col in enumerate(columns_clean) if col.startswith("BDV_T")]
    for idx in bdv_t_indices:
        plt.axvline(x=idx, color='darkgrey', linestyle='--', linewidth=5, alpha=0.7)

    # Customize the plot
    plt.title("Comparison of Single/MultiArrayMemPool", fontsize=15, loc="left")
    plt.ylabel("Command time (seconds)", fontsize=14)
    plt.xlabel("Individual commands", fontsize=14)
    plt.xticks(x_tics, columns_highlight, rotation=45, ha="right", rotation_mode="anchor", fontsize=10)

    # Add second axis
    spot_row = list_rows_with_spots_counts(csv_table)[0]
    spot_sizes_labels = [ int(csv_table.at[spot_row, col_label]) for col_label in columns ]
    secax = plt.gca().secondary_xaxis(location="top")
    secax.set_xlabel("Number of spots", fontsize=14)
    secax.set_xticks(x_tics, labels=spot_sizes_labels, rotation=45, ha="left", rotation_mode="anchor", fontsize=10)
    # or, using normal horizontal text
    #secax.set_xticks(x_tics, labels=spot_sizes_labels, fontsize=10)

# Disabled for now, as it is drawing black or black... I guess a leftover before the vertical bar came-in
#    # Apply colors to individual x-axis tick labels
#    ax = plt.gca()
#    for tick, col in zip(ax.get_xticklabels(), columns_clean):
#        tick.set_color('black' if col.startswith("BDV_T") else 'black')

    # Add gridlines for better readability
    plt.grid(axis='y', linestyle='--', linewidth=0.5, alpha=0.7)

    # Add internal legend (for datasets and sources)
    plt.legend(loc='upper left', title="Legend")

    # Add external legend (for file information) with matching colors
    plt.gcf().text(1.02, 0.95, "File-to-Dataset Mapping:", fontsize=10, fontweight='bold', ha='left')
    for idx, (label, color) in enumerate(zip(file_legend_labels, file_legend_colors)):
        plt.gcf().text(1.02, 0.9 - idx * 0.05, label, fontsize=10, ha='left', color=color)

    # Save the plot
    output_file = os.path.join(output_dir, "comparison_plot_multiple_sources.png")
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches="tight")
    print(f"Plot saved to: {output_file}")

    # Show the plot
    plt.show()


# Call the function to plot with error bars
plot_with_error_bars(csv_files, sources, output_dir)

