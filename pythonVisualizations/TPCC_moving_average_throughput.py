import pandas as pd
import matplotlib.pyplot as plt

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')

# Create the moving average throughput plot
plt.figure(figsize=(12, 6))
window_size = 10
data['Throughput MA'] = data['Throughput (requests/second)'].rolling(window=window_size).mean()

plt.plot(data['Time (seconds)'], data['Throughput (requests/second)'], 
         alpha=0.4, label='Raw Throughput')
plt.plot(data['Time (seconds)'], data['Throughput MA'], 
         linewidth=2, label=f'Moving Average (window={window_size})')

plt.title(f'Throughput with {window_size}-second Moving Average', fontsize=14)
plt.xlabel('Time (seconds)', fontsize=12)
plt.ylabel('Throughput (requests/second)', fontsize=12)
plt.legend()
plt.grid(True, alpha=0.3)

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print throughput stability metrics
print(f"Standard Deviation of Throughput: {data['Throughput (requests/second)'].std():.2f}")
print(f"Coefficient of Variation: {data['Throughput (requests/second)'].std() / data['Throughput (requests/second)'].mean():.4f}")
print(f"This indicates {'high' if data['Throughput (requests/second)'].std() / data['Throughput (requests/second)'].mean() > 0.3 else 'moderate' if data['Throughput (requests/second)'].std() / data['Throughput (requests/second)'].mean() > 0.1 else 'low'} variability in throughput.")