interface ShieldLogoProps {
  className?: string;
}

export function ShieldLogo({ className = "w-7 h-7" }: ShieldLogoProps) {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      {/* Modern shield shape with rounded bottom */}
      <path
        d="M10 1.5L3.5 4.5V10C3.5 13.5 6.5 17.5 10 18.5C13.5 17.5 16.5 13.5 16.5 10V4.5L10 1.5Z"
        className="fill-primary stroke-primary"
        strokeWidth="0.5"
      />

      {/* Inner highlight */}
      <path
        d="M10 2.5L4.5 5V10C4.5 12.8 7 16.2 10 17C13 16.2 15.5 12.8 15.5 10V5L10 2.5Z"
        className="fill-primary-foreground/90"
      />

      {/* Security icon - lock symbol */}
      <rect
        x="7.5"
        y="9"
        width="5"
        height="4"
        rx="0.5"
        className="fill-primary"
      />
      <path
        d="M8.5 9V7.5C8.5 6.7 9.2 6 10 6C10.8 6 11.5 6.7 11.5 7.5V9"
        className="stroke-primary"
        strokeWidth="1"
        fill="none"
      />
    </svg>
  );
}