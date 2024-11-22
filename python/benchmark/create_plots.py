import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
import os


csv_file = '/home/ulman/data/Mastodon-benchmarkData/benchmark_measurements_testingRounds.csv'
source = "Avg per BigDataViewers"

def list_rows_with_source(table, src:str) -> list[int]:
    return [ i for i,row_header in enumerate(table['source']) if row_header.startswith(src) ]

def list_columns_with_data(table) -> list[str]:
    return [ c for i,c in enumerate(table.columns) if i > 7 ]


csv_table = pd.read_csv(csv_file, delimiter='\t')
x_labels = list_columns_with_data(csv_table)

def plot_as_individual_lines(table, x_labels, source:str):
    x_tics = range(len(x_labels))
    for row_idx in list_rows_with_source(table,source):
        y_values = [ table[col_label][row_idx] for col_label in x_labels ]
        #plt.plot(x_labels,y_values,'-o')
        line_label = source+str(table['round'][row_idx])
        plt.plot(x_tics,y_values,'-o',markersize=3,label=line_label)


def plot_as_bar_plot(table, x_labels, source:str):
    rows_idxs = list_rows_with_source(table,source)
    vals = np.zeros([len(x_labels),len(rows_idxs)], dtype=float) # [commands, rounds]
    #
    for ri,row_idx in enumerate(rows_idxs):
        for ci,col_label in enumerate(x_labels):
            vals[ci,ri] = table[col_label][row_idx]
    #
    stats = np.zeros([len(x_labels),2], dtype=float) # [commands, 2 for mean and std]
    for i in range(stats.shape[0]):
        stats[i,0] = vals[i].mean()
        stats[i,1] = vals[i].std()
    #
    x_tics = range(len(x_labels))
    plt.errorbar(x_tics,stats[:,0], yerr=stats[:,1])


# ----------------------------------------------------------------------
# re.search(r'T(\d+)', col)

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
