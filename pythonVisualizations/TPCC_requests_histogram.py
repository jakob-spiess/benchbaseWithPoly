import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')

# Create the requests histogram
plt.figure(figsize=(10, 6))
sns.histplot(data['Requests'], kde=True, bins=15)
plt.axvline(data['Requests'].mean(), color='red', linestyle='--', 
           label=f'Mean: {data["Requests"].mean():.2f}')
plt.axvline(data['Requests'].median(), color='green', linestyle='-', 
           label=f'Median: {data["Requests"].median():.2f}')

plt.title('Distribution of Requests per Second', fontsize=14)
plt.xlabel('Number of Requests', fontsize=12)
plt.ylabel('Frequency', fontsize=12)
plt.legend()

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print requests statistics
print(f"Average Requests per Second: {data['Requests'].mean():.2f}")
print(f"Median Requests per Second: {data['Requests'].median():.2f}")
print(f"Min Requests per Second: {data['Requests'].min()}")
print(f"Max Requests per Second: {data['Requests'].max()}")