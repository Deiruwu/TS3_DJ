class TimeFormatter:
    """
    Formatea duraciones de audio a formato estandar (segundos).
    """

    @staticmethod
    def to_seconds(duration_str):
        """
        Convierte string de duracion a segundos.
        Soporta formatos: "3:45", "1:23:45", "225"
        Returns: int segundos
        """
        if not duration_str:
            return 0

        duration_str = str(duration_str).strip()

        if ':' not in duration_str:
            try:
                return int(duration_str)
            except ValueError:
                return 0

        parts = duration_str.split(':')

        try:
            if len(parts) == 2:
                minutes = int(parts[0])
                seconds = int(parts[1])
                return minutes * 60 + seconds

            elif len(parts) == 3:
                hours = int(parts[0])
                minutes = int(parts[1])
                seconds = int(parts[2])
                return hours * 3600 + minutes * 60 + seconds

            else:
                return 0

        except ValueError:
            return 0