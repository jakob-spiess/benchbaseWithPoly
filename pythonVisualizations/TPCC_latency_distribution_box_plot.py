import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')
sns.set_palette("viridis")

# Create the latency distribution box plot
plt.figure(figsize=(10, 6))
latency_data = data[['Minimum Latency (microseconds)', 
                     '25th Percentile Latency (microseconds)',
                     'Median Latency (microseconds)', 
                     '75th Percentile Latency (microseconds)',
                     '95th Percentile Latency (microseconds)',
                     '99th Percentile Latency (microseconds)',
                     'Maximum Latency (microseconds)']]
latency_data = latency_data / 1000  # Convert to milliseconds
latency_data.columns = ['Min', '25th', 'Median', '75th', '95th', '99th', 'Max']

sns.boxplot(data=latency_data)
plt.title('Latency Distribution (Entire Test)', fontsize=14)
plt.ylabel('Latency (milliseconds)', fontsize=12)
plt.grid(True, alpha=0.3)
plt.yscale('log')  # Log scale for better visualization
plt.ylim(bottom=1)

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print statistics
print("Latency Statistics (milliseconds):")
for column in latency_data.columns:
    print(f"{column} Latency: Mean={latency_data[column].mean():.2f}, Median={latency_data[column].median():.2f}")