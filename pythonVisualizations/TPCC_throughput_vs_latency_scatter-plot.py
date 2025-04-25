import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')

# Create the throughput vs latency scatter plot
plt.figure(figsize=(10, 6))
sc = plt.scatter(data['Throughput (requests/second)'], 
                data['Median Latency (microseconds)']/1000, 
                c=data['Time (seconds)'], cmap='viridis', alpha=0.7)
plt.colorbar(sc, label='Time (seconds)')
plt.title('Throughput vs Median Latency', fontsize=14)
plt.xlabel('Throughput (requests/second)', fontsize=12)
plt.ylabel('Median Latency (milliseconds)', fontsize=12)
plt.grid(True, alpha=0.3)

# Add a trend line
z = np.polyfit(data['Throughput (requests/second)'], data['Median Latency (microseconds)']/1000, 1)
p = np.poly1d(z)
x_range = np.linspace(data['Throughput (requests/second)'].min(), data['Throughput (requests/second)'].max(), 100)
plt.plot(x_range, p(x_range), "r--", alpha=0.7, 
         label=f'Trend: {z[0]:.2f}x + {z[1]:.2f}')
plt.legend()

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Calculate correlation coefficient
corr = np.corrcoef(data['Throughput (requests/second)'], data['Median Latency (microseconds)'])[0,1]
print(f"Correlation between Throughput and Median Latency: {corr:.4f}")
print(f"This suggests a {'strong' if abs(corr) > 0.7 else 'moderate' if abs(corr) > 0.3 else 'weak'} {'positive' if corr > 0 else 'negative'} relationship.")